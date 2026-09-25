#!/usr/bin/env python3
"""Writes the structure templates in the formats older Minecraft versions read:
    python3 tools/downgrade_templates.py

Reads src/main/resources/data/mimicry/structure/*.nbt (26.3) and writes src/main/overlays/<version>/data/mimicry/structure/.
Run it again whenever a template changes.
"""
import json
import os

import nbt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, 'src/main/resources/data/mimicry/structure')


def old_palette_keys(template):
    """26.3 palette entries are {id, properties}; earlier versions read {Name, Properties}."""
    for entry in template['palette']:
        values = entry.value
        values['Name'] = values.pop('id')
        if 'properties' in values:
            values['Properties'] = values.pop('properties')


def to_26_1(template):
    old_palette_keys(template)
    template.value['DataVersion'] = nbt.int_(4786)


def plain(tag):
    """An NBT text component as the JSON value 1.21.1 stores it as."""
    if tag.type == nbt.COMPOUND:
        return {k: plain(v) for k, v in tag.value.items()}
    if tag.type == nbt.LIST:
        return [plain(v) for v in tag.value[1]]
    if tag.type == nbt.BYTE:
        return bool(tag.value)
    return tag.value


def json_text_lines(text):
    for key in ('messages', 'filtered_messages'):
        if key in text:
            lines = [nbt.string(json.dumps(plain(line), separators=(',', ':'))) for line in text[key]]
            text.value[key] = nbt.list_(nbt.STRING, lines)


# equipment slots in ArmorStand's 1.21.1 ArmorItems / HandItems order
ARMOR_SLOTS = ('feet', 'legs', 'chest', 'head')
HAND_SLOTS = ('mainhand', 'offhand')
OLDER_ATTRIBUTES = {'minecraft:movement_speed': 'minecraft:generic.movement_speed', 'minecraft:armor': 'minecraft:generic.armor'}


def to_1_21_1(template):
    template.value['DataVersion'] = nbt.int_(3955)
    for entry in template['palette']:
        if entry['Name'].value == 'minecraft:iron_chain':
            entry.value['Name'] = nbt.string('minecraft:chain')
    for entity in template['entities']:
        data = entity['nbt'].value
        if 'fall_distance' in data:
            data['FallDistance'] = nbt.Tag(nbt.FLOAT, float(data.pop('fall_distance').value))
        if 'block_pos' in data:
            x, y, z = data.pop('block_pos').value
            data['TileX'], data['TileY'], data['TileZ'] = nbt.int_(x), nbt.int_(y), nbt.int_(z)
        if 'equipment' in data:
            equipment = data.pop('equipment')
            stack = lambda slot: equipment.get(slot) or nbt.compound()
            data['ArmorItems'] = nbt.list_(nbt.COMPOUND, [stack(slot) for slot in ARMOR_SLOTS])
            data['HandItems'] = nbt.list_(nbt.COMPOUND, [stack(slot) for slot in HAND_SLOTS])
        for attribute in data.get('attributes', []):
            name = attribute['id'].value
            attribute.value['id'] = nbt.string(OLDER_ATTRIBUTES.get(name, name))
    for block in template['blocks']:
        data = block.get('nbt')
        if data is None:
            continue
        for side in ('front_text', 'back_text'):
            if side in data:
                json_text_lines(data[side])
        # 26.x furnace timers; 1.21.1 reads BurnTime/CookTime/CookTimeTotal, which default to 0
        for key in ('cooking_time_spent', 'cooking_total_time', 'lit_time_remaining', 'lit_total_time', 'speed_multiplier'):
            data.value.pop(key, None)


# blocks and paintings that 1.20.1 doesn't have
OLDER_BLOCKS = {'minecraft:short_grass': 'minecraft:grass', 'minecraft:tuff_bricks': 'minecraft:stone_bricks'}
OLDER_PAINTINGS = {'minecraft:cavebird': 'minecraft:bust'}
PATTERN_CODES = {'minecraft:rhombus': 'mr', 'minecraft:circle': 'mc', 'minecraft:border': 'bo', 'minecraft:curly_border': 'cbo'}
COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
          'brown', 'green', 'red', 'black']


def old_stacks(tag):
    """1.20.5+ item stacks ({id, count}) to 1.20.1 ones ({id, Count})."""
    if tag.type == nbt.COMPOUND:
        if 'id' in tag and 'count' in tag:
            tag.value['Count'] = nbt.Tag(nbt.BYTE, tag.value.pop('count').value)
        for value in tag.value.values():
            old_stacks(value)
    elif tag.type == nbt.LIST:
        for value in tag.value[1]:
            old_stacks(value)


def to_1_20_1(template):
    template.value['DataVersion'] = nbt.int_(3465)
    for entry in template['palette']:
        name = entry['Name'].value
        if name in OLDER_BLOCKS:
            entry.value['Name'] = nbt.string(OLDER_BLOCKS[name])
    for entity in template['entities']:
        data = entity['nbt']
        data.value.pop('attributes', None)
        if 'variant' in data and data['variant'].value in OLDER_PAINTINGS:
            data.value['variant'] = nbt.string(OLDER_PAINTINGS[data['variant'].value])
        old_stacks(data)
    for block in template['blocks']:
        data = block.get('nbt')
        if data is None:
            continue
        data.value.pop('components', None)
        if 'patterns' in data:
            patterns = [nbt.compound(Pattern=nbt.string(PATTERN_CODES[p['pattern'].value]), Color=nbt.int_(COLORS.index(p['color'].value)))
                        for p in data.value.pop('patterns')]
            data.value['Patterns'] = nbt.list_(nbt.COMPOUND, patterns)
        old_stacks(data)


# overlay directory: the newest version the format is for, and how to get there from the previous one
TARGETS = [('26.2', to_26_1), ('1.21.1', to_1_21_1), ('1.20.1', to_1_20_1)]


def main():
    for name in sorted(os.listdir(SOURCE)):
        if not name.endswith('.nbt'):
            continue
        template = nbt.load(os.path.join(SOURCE, name))
        for overlay, convert in TARGETS:
            convert(template)
            out = os.path.join(ROOT, 'src/main/overlays', overlay, 'data/mimicry/structure', name)
            os.makedirs(os.path.dirname(out), exist_ok=True)
            nbt.write_gzip(out, template)
            print(out)


if __name__ == '__main__':
    main()
