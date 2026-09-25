#!/usr/bin/env python3
"""
Turns the hand-edited builds in the "Hollowmere Workshop" world back into the mod's structure templates:
    python3 tools/import_workshop.py [path/to/Hollowmere Workshop] [output dir]
Knights, mimics and the blacksmith become DATA markers, bedrock placed above the ground marks an extra knight's post,
and Mimic Chests become keep hoard chests.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import nbt  # noqa: E402
import workshop  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'mimicry', 'structure')
DEFAULT_WORLD = os.path.join(ROOT, 'run', 'saves', 'Hollowmere Workshop')

# name: (origin of the outline in the workshop world, size); must match Workshop.java
BUILDS = {
    'castle': ((43, -64, -13), (43, 34, 43)),
    'wayside_forge': ((0, -60, 0), (23, 19, 21)),
}
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'}
CONTAINERS = {'minecraft:chest', 'minecraft:barrel', 'minecraft:trapped_chest'}
HOARD = 'mimicry:chests/sunken_keep_hoard'
MARKERS = {'mimicry:moss_knight': 'knight', 'mimicry:mimic': 'mimic', 'mimicry:blacksmith': 'blacksmith'}


def island(u, v):
    """Rounded-square distance from the centre, same shape as CastlePiece's moat."""
    au, av = abs(u), abs(v)
    return 14 + math.hypot(au - 14, av - 14) if au > 14 and av > 14 else max(au, av)


def keeps_block(name, pos, block_state):
    """Castle: skip the flat world and all air, since template air would erase what the cavern builds there."""
    if name != 'castle':
        return True
    x, y, z = pos
    u, v = x - 21, z - 21
    if island(u, v) > 21.2:
        return False
    if block_state[0] == 'minecraft:bedrock' and y == 0:
        return False
    return block_state[0] not in AIR


def marker(metadata):
    state = ('minecraft:structure_block', (('mode', 'data'),))
    data = nbt.compound(id=nbt.string('minecraft:structure_block'), mode=nbt.string('DATA'), metadata=nbt.string(metadata),
                        name=nbt.string(''), author=nbt.string('workshop'))
    return state, data


def fix_container(name, pos, data, pristine):
    lt = data.get('LootTable')
    table = lt.value if lt else None
    if table == 'mimicry:chests/mimic_chest':
        table = HOARD
    elif table is None:
        old = pristine.get(pos)
        old_table = old[1].get('LootTable') if old and old[1] is not None else None
        if old_table is not None:
            table = old_table.value  # opened while editing
        elif data['id'].value == 'minecraft:chest' and name == 'castle':
            table = 'mimicry:chests/sunken_keep'
    for key in ('LootTable', 'LootTableSeed', 'Items'):
        data.value.pop(key, None)
    if table:
        data.value['LootTable'] = nbt.string(table)
    return table


def fix_lectern(data):
    """Swaps the old written-book almanac for the mimicry:almanac item."""
    book = data.get('Book')
    content = book.get('components', nbt.compound()).get('minecraft:written_book_content') if book is not None else None
    title = content.get('title') if content is not None else None
    if title is not None and title.get('raw') is not None and title['raw'].value == 'Almanac of Hollowmere':
        data.value['Book'] = nbt.compound(count=nbt.int_(1), id=nbt.string('mimicry:almanac'))
        return True
    return False


def build_template(world, name):
    origin, size = BUILDS[name]
    blocks, block_entities, entities = workshop.read_box(world, origin, size)
    pristine_template = nbt.load(os.path.join(OUT, f'{name}.nbt'))
    pristine = workshop.template_blocks(pristine_template)

    placed = {}
    report = {'knights': [], 'markers': [], 'hoards': 0, 'restored': 0, 'entities': []}
    for pos, block_state in blocks.items():
        if block_state[0] == 'minecraft:bedrock' and pos[1] > 0:
            placed[pos] = marker('knight')
            report['knights'].append(pos)
            continue
        if not keeps_block(name, pos, block_state):
            continue
        data = None
        be = block_entities.get(pos)
        if be is not None:
            data = nbt.Tag(nbt.COMPOUND, {k: v for k, v in be.value.items() if k not in ('x', 'y', 'z', 'keepPacked')})
            if be['id'].value in CONTAINERS:
                had = be.get('LootTable')
                table = fix_container(name, pos, data, pristine)
                report['hoards'] += table == HOARD
                report['restored'] += had is None and table is not None
            if be['id'].value == 'minecraft:lectern':
                fix_lectern(data)
        if block_state[0].endswith('_door'):
            block_state = (block_state[0], tuple((k, 'false' if k == 'open' else v) for k, v in block_state[1]))
        placed[pos] = (block_state, data)

    template_entities = []
    for entity in entities:
        id = entity['id'].value
        x, y, z = (v.value for v in entity['Pos'])
        rel = (x - origin[0], y - origin[1], z - origin[2])
        block = tuple(math.floor(c) for c in rel)
        if id in MARKERS:
            placed[block] = marker(MARKERS[id])
            report['markers'].append((MARKERS[id], block))
            continue
        if id == 'minecraft:item':
            continue
        attached = entity.get('block_pos')
        if attached is not None:
            block = tuple(c - o for c, o in zip(attached.value, origin))
        data = nbt.Tag(nbt.COMPOUND, {k: v for k, v in entity.value.items() if k != 'UUID'})
        template_entities.append(nbt.compound(
            pos=nbt.list_(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, c) for c in rel]),
            blockPos=nbt.list_(nbt.INT, [nbt.int_(c) for c in block]),
            nbt=data))
        report['entities'].append(id)

    palette, index = [], {}
    block_list = []
    for pos in sorted(placed, key=lambda p: (p[1], p[2], p[0])):
        block_state, data = placed[pos]
        if block_state not in index:
            index[block_state] = len(palette)
            palette.append(workshop.palette_entry(*block_state))
        entry = nbt.compound(pos=nbt.list_(nbt.INT, [nbt.int_(c) for c in pos]), state=nbt.int_(index[block_state]))
        if data is not None:
            entry.value['nbt'] = data
        block_list.append(entry)
    template = nbt.compound(
        size=nbt.list_(nbt.INT, [nbt.int_(c) for c in size]),
        entities=nbt.list_(nbt.COMPOUND, template_entities),
        blocks=nbt.list_(nbt.COMPOUND, block_list),
        palette=nbt.list_(nbt.COMPOUND, palette),
        DataVersion=pristine_template['DataVersion'])
    return template, report, len(block_list)


def main():
    world = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_WORLD
    out = sys.argv[2] if len(sys.argv) > 2 else OUT
    os.makedirs(out, exist_ok=True)
    for name in BUILDS:
        template, report, count = build_template(world, name)
        nbt.write_gzip(os.path.join(out, f'{name}.nbt'), template)
        print(f'{name}: {count} blocks, knight posts {sorted(report["knights"])}, markers {report["markers"]}, '
              f'{report["hoards"]} hoard chests, {report["restored"]} loot tables restored, entities {report["entities"]}')


if __name__ == '__main__':
    main()
