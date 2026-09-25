"""
Reads a box of blocks, block entities and entities straight out of a world save (Anvil region files), in the same
layout as a structure template.
"""
import glob
import math
import os

import nbt


def state(entry):
    """Returns (name, ((property, value), ...)). 26.x writes {id, properties} or a bare name ({"": name} in a
    mixed list); older saves wrote {Name, Properties}."""
    if entry.type == nbt.STRING:
        return entry.value, ()
    if '' in entry:
        return entry[''].value, ()
    name = (entry.get('id') or entry.get('Name')).value
    props = entry.get('properties') or entry.get('Properties')
    return name, tuple(sorted((k, v.value) for k, v in props.value.items())) if props else ()


def palette_entry(name, props):
    """A 26.x template palette entry."""
    entry = nbt.compound(id=nbt.string(name))
    if props:
        entry.value['properties'] = nbt.Tag(nbt.COMPOUND, {k: nbt.string(v) for k, v in props})
    return entry


def read_box(world, origin, size, dimension='minecraft/overworld'):
    """Returns (blocks, block_entities, entities); block positions are relative to origin."""
    ox, oy, oz = origin
    sx, sy, sz = size
    blocks, block_entities, entities = {}, {}, []
    region_dir = os.path.join(world, 'dimensions', dimension, 'region')
    for path in glob.glob(os.path.join(region_dir, 'r.*.mca')):
        for chunk in nbt.region_chunks(path):
            cx, cz = chunk['xPos'].value, chunk['zPos'].value
            if cx * 16 + 15 < ox or cx * 16 > ox + sx - 1 or cz * 16 + 15 < oz or cz * 16 > oz + sz - 1:
                continue
            for section in chunk.get('sections', nbt.list_(nbt.COMPOUND, [])):
                states = section.get('block_states')
                if states is None:
                    continue
                sy0 = section['Y'].value * 16
                if sy0 + 15 < oy or sy0 > oy + sy - 1:
                    continue
                palette = list(states['palette'])
                data = states.get('data')
                bits = max(4, math.ceil(math.log2(len(palette)))) if len(palette) > 1 else 0
                per_long = 64 // bits if bits else 0
                for i in range(4096):
                    x, z, y = i & 15, (i >> 4) & 15, i >> 8
                    wx, wy, wz = cx * 16 + x, sy0 + y, cz * 16 + z
                    if not (ox <= wx < ox + sx and oy <= wy < oy + sy and oz <= wz < oz + sz):
                        continue
                    if bits:
                        word = data.value[i // per_long] & 0xFFFFFFFFFFFFFFFF
                        index = (word >> ((i % per_long) * bits)) & ((1 << bits) - 1)
                    else:
                        index = 0
                    blocks[(wx - ox, wy - oy, wz - oz)] = state(palette[index])
            for be in chunk.get('block_entities', nbt.list_(nbt.COMPOUND, [])):
                p = (be['x'].value - ox, be['y'].value - oy, be['z'].value - oz)
                if p in blocks:
                    block_entities[p] = be
    entity_dir = os.path.join(world, 'dimensions', dimension, 'entities')
    for path in glob.glob(os.path.join(entity_dir, 'r.*.mca')):
        for chunk in nbt.region_chunks(path):
            for entity in chunk.get('Entities', nbt.list_(nbt.COMPOUND, [])):
                x, y, z = (v.value for v in entity['Pos'])
                if ox <= x < ox + sx and oy <= y < oy + sy and oz <= z < oz + sz:
                    entities.append(entity)
    return blocks, block_entities, entities


def template_blocks(template):
    palette = [state(e) for e in template['palette']]
    return {tuple(v.value for v in b['pos']): (palette[b['state'].value], b.get('nbt')) for b in template['blocks']}


