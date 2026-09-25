"""
Minimal NBT reader/writer (gzip or raw) plus Anvil region chunks. Every value is a Tag(type, value) so files
round-trip exactly: a compound's value is a dict of name -> Tag, a list's is (element type, [Tag, ...]).
"""
import gzip
import io
import struct
import zlib

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)
_SCALAR = {BYTE: '>b', SHORT: '>h', INT: '>i', LONG: '>q', FLOAT: '>f', DOUBLE: '>d'}


class Tag:
    __slots__ = ('type', 'value')

    def __init__(self, type, value):
        self.type = type
        self.value = value

    def __repr__(self):
        return f'Tag({self.type}, {self.value!r})'

    def __getitem__(self, key):
        if self.type == COMPOUND:
            return self.value[key]
        return self.value[1][key]

    def get(self, key, default=None):
        return self.value.get(key, default) if self.type == COMPOUND else default

    def __contains__(self, key):
        return self.type == COMPOUND and key in self.value

    def __iter__(self):
        return iter(self.value[1] if self.type == LIST else self.value)

    def __len__(self):
        return len(self.value[1] if self.type == LIST else self.value)

    def plain(self):
        if self.type == COMPOUND:
            return {k: v.plain() for k, v in self.value.items()}
        if self.type == LIST:
            return [v.plain() for v in self.value[1]]
        return self.value


def compound(**items):
    return Tag(COMPOUND, dict(items))


def string(s):
    return Tag(STRING, s)


def int_(i):
    return Tag(INT, int(i))


def list_(type, items):
    return Tag(LIST, (type if items else END, list(items)))


def _read_payload(f, type):
    if type in _SCALAR:
        fmt = _SCALAR[type]
        return struct.unpack(fmt, f.read(struct.calcsize(fmt)))[0]
    if type == BYTE_ARRAY:
        n = struct.unpack('>i', f.read(4))[0]
        return bytearray(f.read(n))
    if type == STRING:
        n = struct.unpack('>H', f.read(2))[0]
        return f.read(n).decode('utf-8', 'surrogatepass')
    if type == LIST:
        element = struct.unpack('>b', f.read(1))[0]
        n = struct.unpack('>i', f.read(4))[0]
        return (element, [Tag(element, _read_payload(f, element)) for _ in range(n)])
    if type == COMPOUND:
        items = {}
        while True:
            t = struct.unpack('>b', f.read(1))[0]
            if t == END:
                return items
            name = _read_payload(f, STRING)
            items[name] = Tag(t, _read_payload(f, t))
    if type == INT_ARRAY:
        n = struct.unpack('>i', f.read(4))[0]
        return list(struct.unpack(f'>{n}i', f.read(4 * n)))
    if type == LONG_ARRAY:
        n = struct.unpack('>i', f.read(4))[0]
        return list(struct.unpack(f'>{n}q', f.read(8 * n)))
    raise ValueError(f'bad tag type {type}')


def _write_payload(f, tag):
    type, value = tag.type, tag.value
    if type in _SCALAR:
        f.write(struct.pack(_SCALAR[type], value))
    elif type == BYTE_ARRAY:
        f.write(struct.pack('>i', len(value)) + bytes(value))
    elif type == STRING:
        data = value.encode('utf-8', 'surrogatepass')
        f.write(struct.pack('>H', len(data)) + data)
    elif type == LIST:
        element, items = value
        f.write(struct.pack('>bi', element if items else END, len(items)))
        for item in items:
            _write_payload(f, item)
    elif type == COMPOUND:
        for name, item in value.items():
            f.write(struct.pack('>b', item.type))
            _write_payload(f, Tag(STRING, name))
            _write_payload(f, item)
        f.write(struct.pack('>b', END))
    elif type == INT_ARRAY:
        f.write(struct.pack(f'>i{len(value)}i', len(value), *value))
    elif type == LONG_ARRAY:
        f.write(struct.pack(f'>i{len(value)}q', len(value), *value))
    else:
        raise ValueError(f'bad tag type {type}')


def read(data):
    if data[:2] == b'\x1f\x8b':
        data = gzip.decompress(data)
    f = io.BytesIO(data)
    type = struct.unpack('>b', f.read(1))[0]
    _read_payload(f, STRING)
    return Tag(type, _read_payload(f, type))


def write_gzip(path, root):
    f = io.BytesIO()
    f.write(struct.pack('>b', root.type))
    _write_payload(f, Tag(STRING, ''))
    _write_payload(f, root)
    with open(path, 'wb') as out:
        out.write(gzip.compress(f.getvalue(), mtime=0))


def load(path):
    with open(path, 'rb') as f:
        return read(f.read())


def region_chunks(path):
    with open(path, 'rb') as f:
        data = f.read()
    for i in range(1024):
        offset = int.from_bytes(data[i * 4:i * 4 + 3], 'big')
        if not offset:
            continue
        length = struct.unpack('>I', data[offset * 4096:offset * 4096 + 4])[0]
        compression = data[offset * 4096 + 4]
        raw = data[offset * 4096 + 5:offset * 4096 + 4 + length]
        yield read(zlib.decompress(raw) if compression == 2 else gzip.decompress(raw) if compression == 1 else raw)
