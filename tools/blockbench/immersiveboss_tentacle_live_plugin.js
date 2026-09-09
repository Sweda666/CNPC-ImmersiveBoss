Plugin.register('immersiveboss_tentacle_live', {
  title: 'ImmersiveBoss Tentacle (Live)',
  author: 'CNPC ImmersiveBoss',
  description: 'Adds the textured animated tentacle to the currently open GeckoLib project.',
  icon: 'pets',
  version: '1.0.0',
  onload: function () {
    var fs = require('fs');
    var source = fs.readFileSync('F:/idea/develops/CNPC-ImmersiveBoss-1.20.1/tools/blockbench/apply_tentacle_to_open_project.js', 'utf8');
    eval(source);
  },
  onunload: function () {}
});
