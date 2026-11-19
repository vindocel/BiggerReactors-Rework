// Added 2026-11-01: runtime fixture for runGameTestScripting; never distributed with the mod.
BiggerReactorsEvents.config(event => {
  event.set('Worldgen.GenerateUranium', false)
  event.set('Battery.CellCapacity', 3200001000)
  event.set('Battery.CellTransferRate', 300123)
  event.fuel('minecraft:apple', 2500)
})
BiggerReactorsEvents.materials(event => {
  event.coil('minecraft:honey_block', 1.25, 1.1, 1.0)
  event.moderator('minecraft:sponge', 0.15, 0.9, 2.0, 1.0)
  event.fuel('minecraft:beetroot', 1234)
  event.transition('minecraft:lava', 'biggerreactorsrework:heated_coolant', 12, 1300, 2, 2, 0, 1)
})
