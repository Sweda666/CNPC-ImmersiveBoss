(function() {
    'use strict';

    const PLUGIN_ID = 'cnpc_immersiveboss_hitbox_preview';
    const SETTING_ENABLED = PLUGIN_ID + '_enabled';
    const SETTING_AXES = PLUGIN_ID + '_axes';
    const SETTING_RUNTIME_VISIBILITY = PLUGIN_ID + '_runtime_visibility';
    const SETTING_HIDE_ALL = PLUGIN_ID + '_hide_all';
    const PLAYER_RIG_ACTION = PLUGIN_ID + '_create_player_rig';
    const PLAYER_SKIN_ACTION = PLUGIN_ID + '_set_player_skin';
    const CAMERA_PREVIEW_PANEL = PLUGIN_ID + '_camera_preview';
    const DEFAULT_PLAYER_SKIN = 'Sweda';
    const I18N = {
        en: {
            'cnpc_immersiveboss.plugin.title': 'CNPC ImmersiveBoss Hitbox Preview',
            'cnpc_immersiveboss.plugin.description': 'Preview CNPC ImmersiveBoss hitbox bones as colored in-game wireframes.',
            'cnpc_immersiveboss.plugin.about': 'Shows every cube directly inside a valid h[a][d][b|s]_name bone as a colored OBB wireframe.\n\nWhite = physical + detectable; Blue = physical; Yellow = detectable sensor; Green = sensor.\n\nUse Hide All IB hitboxes to hide hitbox groups and cubes. Create IB Player Rig adds the victim_root player puppet and hand-item attachment groups. Set Official Minecraft Player Skin previews a Mojang Java skin.\n\nAll previews are viewport-only and never change exported model geometry.',
            'cnpc_immersiveboss.settings.preview.name': 'ImmersiveBoss Hitbox Preview',
            'cnpc_immersiveboss.settings.preview.description': 'Show runtime-style wireframes for ImmersiveBoss hitbox bones.',
            'cnpc_immersiveboss.settings.axes.name': 'ImmersiveBoss Hitbox Axes',
            'cnpc_immersiveboss.settings.axes.description': 'Show local red, green, and blue axes inside each hitbox.',
            'cnpc_immersiveboss.settings.runtime.name': 'ImmersiveBoss Runtime Visibility',
            'cnpc_immersiveboss.settings.runtime.description': 'Hide cube faces for hitbox bones without the appearance flag.',
            'cnpc_immersiveboss.settings.hide_all.name': 'Hide All IB hitboxes',
            'cnpc_immersiveboss.settings.hide_all.description': 'Make all ImmersiveBoss hitbox groups and cubes invisible and unpickable in the viewport.',
            'cnpc_immersiveboss.actions.hide_all.name': 'Hide All IB hitboxes',
            'cnpc_immersiveboss.actions.hide_all.description': 'Hide ImmersiveBoss hitbox groups and cubes from rendering and viewport selection.',
            'cnpc_immersiveboss.actions.player_skin.name': 'Set Official Minecraft Player Skin',
            'cnpc_immersiveboss.actions.player_skin.description': 'Fetch a Java Edition player skin from Mojang and apply it to victim_root.',
            'cnpc_immersiveboss.actions.player_rig.name': 'Create IB Player Rig',
            'cnpc_immersiveboss.actions.player_rig.description': 'Create or complete victim_root at its current position, including skin layers, item groups, and throw cameras.',
            'cnpc_immersiveboss.camera_preview.name': 'Camera View Preview',
            'cnpc_immersiveboss.camera_preview.select': 'Preview camera view',
            'cnpc_immersiveboss.camera_preview.first_person': 'First Person',
            'cnpc_immersiveboss.camera_preview.second_person': 'Second Person',
            'cnpc_immersiveboss.camera_preview.third_person': 'Third Person',
            'cnpc_immersiveboss.camera_preview.missing': 'Camera bone %0 was not found. Run Create IB Player Rig first.',
            'cnpc_immersiveboss.dialog.skin.title': 'Set Official Minecraft Player Skin',
            'cnpc_immersiveboss.dialog.skin.line1': 'Enter a Minecraft Java username. The plugin fetches the current skin from Mojang.',
            'cnpc_immersiveboss.dialog.skin.line2': 'The skin is used only for the Blockbench preview and is never exported.',
            'cnpc_immersiveboss.dialog.skin.username': 'Minecraft Java username',
            'cnpc_immersiveboss.messages.title': 'CNPC ImmersiveBoss',
            'cnpc_immersiveboss.messages.no_victim': 'This project has no victim_root player model. Run Create IB Player Rig first.',
            'cnpc_immersiveboss.messages.loading_skin': 'Fetching player skin from Mojang…',
            'cnpc_immersiveboss.messages.skin_applied': 'Applied %0\'s official skin to victim_root.',
            'cnpc_immersiveboss.messages.skin_failed': 'Failed to fetch the official player skin: %0',
            'cnpc_immersiveboss.messages.enter_username': 'Enter a Minecraft Java username.',
            'cnpc_immersiveboss.messages.open_model': 'Open a GeckoLib/Bedrock model project first.',
            'cnpc_immersiveboss.messages.camera_plugin': 'Blockbench does not provide Camera components. Enable the Cameras plugin first.',
            'cnpc_immersiveboss.messages.fixed_arms': 'Fixed the legacy reversed victim arm layout.',
            'cnpc_immersiveboss.messages.missing_bones': 'Cannot complete the player rig; missing bones: %0',
            'cnpc_immersiveboss.messages.camera_failed': 'Unable to create throw cameras. Check the Blockbench Cameras plugin.',
            'cnpc_immersiveboss.messages.rig_exists': 'This project already contains a complete victim_root; nothing was created.',
            'cnpc_immersiveboss.messages.camera_compatibility': 'Camera component creation failed. Check the Cameras plugin version.',
            'cnpc_immersiveboss.messages.rig_created': 'Created the victim_root player rig with three camera views.',
            'cnpc_immersiveboss.messages.rig_completed_cameras': 'Completed the missing victim_root parts at the existing rig position.',
            'cnpc_immersiveboss.messages.rig_completed_layers': 'Completed the missing second-layer player skin geometry.',
            'cnpc_immersiveboss.undo.fix_arms': 'Fix reversed IB player arms',
            'cnpc_immersiveboss.undo.complete_rig': 'Complete missing IB player rig parts',
            'cnpc_immersiveboss.undo.create_rig': 'Create IB Player Rig',
            'cnpc_immersiveboss.errors.viewport_texture': 'This Blockbench version cannot create viewport skin materials.',
            'cnpc_immersiveboss.errors.skin_image': 'Failed to read the player skin image.',
            'cnpc_immersiveboss.errors.network': 'This Blockbench version cannot make network requests. Upgrade to 4.10 or newer.',
            'cnpc_immersiveboss.errors.decode_profile': 'Could not decode Mojang skin profile data.',
            'cnpc_immersiveboss.errors.image_data': 'This Blockbench version cannot convert image data.',
            'cnpc_immersiveboss.errors.player_not_found': 'Official player not found: %0',
            'cnpc_immersiveboss.errors.profile_missing': 'Mojang did not return skin profile data for this player.',
            'cnpc_immersiveboss.errors.skin_missing': 'This player does not have an available official skin.',
            'cnpc_immersiveboss.errors.skin_download': 'Failed to download the player skin image (HTTP %0).'
        },
        zh: {
            'cnpc_immersiveboss.plugin.title': 'CNPC ImmersiveBoss 碰撞箱预览',
            'cnpc_immersiveboss.plugin.description': '以彩色线框预览 CNPC ImmersiveBoss 的碰撞箱骨骼。',
            'cnpc_immersiveboss.plugin.about': '将有效 h[a][d][b|s]_名称 骨骼中的每个方块显示为彩色 OBB 线框。\n\n白色 = 物理且可检测；蓝色 = 物理；黄色 = 可检测传感器；绿色 = 传感器。\n\n使用 Hide All IB hitboxes 隐藏碰撞箱组和方块。Create IB Player Rig 创建 victim_root 玩家替身及手持物品组；Set Official Minecraft Player Skin 预览 Mojang Java 版皮肤。\n\n所有预览仅存在于视口，不会修改导出的模型几何体。',
            'cnpc_immersiveboss.settings.preview.name': 'ImmersiveBoss 碰撞箱预览',
            'cnpc_immersiveboss.settings.preview.description': '显示与游戏运行时一致的 ImmersiveBoss 碰撞箱线框。',
            'cnpc_immersiveboss.settings.axes.name': 'ImmersiveBoss 碰撞箱坐标轴',
            'cnpc_immersiveboss.settings.axes.description': '在每个碰撞箱内显示红、绿、蓝局部坐标轴。',
            'cnpc_immersiveboss.settings.runtime.name': 'ImmersiveBoss 运行时可见性',
            'cnpc_immersiveboss.settings.runtime.description': '隐藏未带外观标记的碰撞箱表面。',
            'cnpc_immersiveboss.settings.hide_all.name': '隐藏所有 IB 碰撞箱',
            'cnpc_immersiveboss.settings.hide_all.description': '在视口中隐藏并禁止选中所有 ImmersiveBoss 碰撞箱组和方块。',
            'cnpc_immersiveboss.actions.hide_all.name': '隐藏所有 IB 碰撞箱',
            'cnpc_immersiveboss.actions.hide_all.description': '从渲染和视口选取中隐藏 ImmersiveBoss 碰撞箱组和方块。',
            'cnpc_immersiveboss.actions.player_skin.name': '设置正版 Minecraft 玩家皮肤',
            'cnpc_immersiveboss.actions.player_skin.description': '从 Mojang 获取 Java 版玩家皮肤并应用到 victim_root。',
            'cnpc_immersiveboss.actions.player_rig.name': '创建 IB 玩家替身',
            'cnpc_immersiveboss.actions.player_rig.description': '在当前替身位置创建或补全 victim_root，包括皮肤层、手持物品组和投技摄像机。',
            'cnpc_immersiveboss.camera_preview.name': '摄像机视角预览',
            'cnpc_immersiveboss.camera_preview.select': '选择预览摄像机视角',
            'cnpc_immersiveboss.camera_preview.first_person': '第一人称',
            'cnpc_immersiveboss.camera_preview.second_person': '第二人称',
            'cnpc_immersiveboss.camera_preview.third_person': '第三人称',
            'cnpc_immersiveboss.camera_preview.missing': '未找到摄像机骨骼 %0，请先执行“创建 IB 玩家替身”。',
            'cnpc_immersiveboss.dialog.skin.title': '设置正版 Minecraft 玩家皮肤',
            'cnpc_immersiveboss.dialog.skin.line1': '输入 Minecraft Java 玩家名，插件会从 Mojang 获取当前皮肤。',
            'cnpc_immersiveboss.dialog.skin.line2': '皮肤只用于 Blockbench 预览，不会被导出。',
            'cnpc_immersiveboss.dialog.skin.username': 'Minecraft Java 玩家名',
            'cnpc_immersiveboss.messages.title': 'CNPC ImmersiveBoss',
            'cnpc_immersiveboss.messages.no_victim': '当前工程没有 victim_root 玩家模型，请先执行“创建 IB 玩家替身”。',
            'cnpc_immersiveboss.messages.loading_skin': '正在从 Mojang 获取玩家皮肤…',
            'cnpc_immersiveboss.messages.skin_applied': '已将 %0 的正版皮肤应用到 victim_root。',
            'cnpc_immersiveboss.messages.skin_failed': '获取正版玩家皮肤失败：%0',
            'cnpc_immersiveboss.messages.enter_username': '请输入 Minecraft Java 玩家名。',
            'cnpc_immersiveboss.messages.open_model': '请先打开 GeckoLib/Bedrock 模型工程。',
            'cnpc_immersiveboss.messages.camera_plugin': '当前 Blockbench 未提供 Camera 组件，请先启用 Cameras 插件。',
            'cnpc_immersiveboss.messages.fixed_arms': '已修复旧版投技玩家左右手臂方向。',
            'cnpc_immersiveboss.messages.missing_bones': '无法完成玩家替身，缺少骨骼：%0',
            'cnpc_immersiveboss.messages.camera_failed': '无法创建投技摄像机，请检查 Blockbench Cameras 插件。',
            'cnpc_immersiveboss.messages.rig_exists': '当前工程已经存在完整的 victim_root，未创建新内容。',
            'cnpc_immersiveboss.messages.camera_compatibility': 'Camera 组件创建失败，请检查 Cameras 插件版本。',
            'cnpc_immersiveboss.messages.rig_created': '已创建包含三个摄像机视角的 victim_root 玩家替身。',
            'cnpc_immersiveboss.messages.rig_completed_cameras': '已在现有替身位置补全 victim_root 的缺失部分。',
            'cnpc_immersiveboss.messages.rig_completed_layers': '已补全缺失的第二层玩家皮肤几何体。',
            'cnpc_immersiveboss.undo.fix_arms': '修复 IB 玩家替身左右手臂方向',
            'cnpc_immersiveboss.undo.complete_rig': '补全 IB 玩家替身缺失部分',
            'cnpc_immersiveboss.undo.create_rig': '创建 IB 玩家替身',
            'cnpc_immersiveboss.errors.viewport_texture': '当前 Blockbench 版本不支持创建视口皮肤材质。',
            'cnpc_immersiveboss.errors.skin_image': '读取玩家皮肤图片失败。',
            'cnpc_immersiveboss.errors.network': '当前 Blockbench 版本不支持网络请求，请升级到 4.10 或更高版本。',
            'cnpc_immersiveboss.errors.decode_profile': '无法解码 Mojang 皮肤资料。',
            'cnpc_immersiveboss.errors.image_data': '当前 Blockbench 版本不支持图片数据转换。',
            'cnpc_immersiveboss.errors.player_not_found': '找不到正版玩家：%0',
            'cnpc_immersiveboss.errors.profile_missing': 'Mojang 没有返回该玩家的皮肤资料。',
            'cnpc_immersiveboss.errors.skin_missing': '该玩家没有可用的正版皮肤。',
            'cnpc_immersiveboss.errors.skin_download': '无法下载玩家皮肤图片（HTTP %0）。'
        }
    };

    function registerTranslations() {
        if (typeof Language === 'undefined' || !Language
            || typeof Language.addTranslations !== 'function') return;
        Object.keys(I18N).forEach(language => Language.addTranslations(language, I18N[language]));
        // Blockbench has used both `zh` and `zh_cn` locale identifiers across
        // releases; register both so the plugin strings follow either one.
        Language.addTranslations('zh_cn', I18N.zh);
    }

    function text(key, fallback, args) {
        if (typeof tl === 'function') {
            const translated = tl(key, args);
            if (translated && translated !== key) return translated;
        }
        let result = fallback || key;
        if (Array.isArray(args)) {
            result = result.replace(/%([0-9]+)/g, (match, index) =>
                args[Number(index)] === undefined ? match : String(args[Number(index)])
            );
        }
        return result;
    }

    registerTranslations();
    const PLAYER_CAMERA_SPECS = [
        {
            bone: 'camera_root',
            camera: 'throw_camera',
            label: 'cnpc_immersiveboss.camera_preview.first_person',
            fallbackLabel: 'First Person',
            origin: [0, 28, -4],
            rotation: [0, 0, 0]
        },
        {
            bone: 'camera_second_person',
            camera: 'throw_camera_second_person',
            label: 'cnpc_immersiveboss.camera_preview.second_person',
            fallbackLabel: 'Second Person',
            origin: [0, 28, -68],
            rotation: [0, 180, 0]
        },
        {
            bone: 'camera_third_person',
            camera: 'throw_camera_third_person',
            label: 'cnpc_immersiveboss.camera_preview.third_person',
            fallbackLabel: 'Third Person',
            origin: [0, 28, 60],
            rotation: [0, 0, 0]
        }
    ];

    const PLAYER_RIG_GROUPS = [
        {name: 'victim_body', origin: [0, 24, 0]},
        {name: 'victim_head', origin: [0, 24, 0]},
        {name: 'victim_right_arm', origin: [5, 22, 0]},
        {name: 'victim_left_arm', origin: [-5, 22, 0]},
        {name: 'victim_right_leg', origin: [-2, 12, 0]},
        {name: 'victim_left_leg', origin: [2, 12, 0]}
    ];

    const PLAYER_BASE_CUBES = [
        {name: 'victim_body_cube', parent: 'victim_body', from: [-4, 12, -2], to: [4, 24, 2], uv: [16, 16]},
        {name: 'victim_head_cube', parent: 'victim_head', from: [-4, 24, -4], to: [4, 32, 4], uv: [0, 0]},
        {name: 'victim_right_arm_cube', parent: 'victim_right_arm', from: [4, 12, -2], to: [8, 24, 2], uv: [40, 16]},
        {name: 'victim_left_arm_cube', parent: 'victim_left_arm', from: [-8, 12, -2], to: [-4, 24, 2], uv: [32, 48]},
        {name: 'victim_right_leg_cube', parent: 'victim_right_leg', from: [-4, 0, -2], to: [0, 12, 2], uv: [0, 16]},
        {name: 'victim_left_leg_cube', parent: 'victim_left_leg', from: [0, 0, -2], to: [4, 12, 2], uv: [16, 48]}
    ];

    const PLAYER_SKIN_LAYERS = [
        {
            name: 'victim_body_layer_cube',
            parent: 'victim_body',
            from: [-4, 12, -2],
            to: [4, 24, 2],
            uv: [16, 32],
            inflate: 0.25
        },
        {
            name: 'victim_head_layer_cube',
            parent: 'victim_head',
            from: [-4, 24, -4],
            to: [4, 32, 4],
            uv: [32, 0],
            inflate: 0.5
        },
        {
            name: 'victim_right_arm_layer_cube',
            parent: 'victim_right_arm',
            from: [4, 12, -2],
            to: [8, 24, 2],
            uv: [40, 32],
            inflate: 0.25
        },
        {
            name: 'victim_left_arm_layer_cube',
            parent: 'victim_left_arm',
            from: [-8, 12, -2],
            to: [-4, 24, 2],
            uv: [48, 48],
            inflate: 0.25
        },
        {
            name: 'victim_right_leg_layer_cube',
            parent: 'victim_right_leg',
            from: [-4, 0, -2],
            to: [0, 12, 2],
            uv: [0, 32],
            inflate: 0.25
        },
        {
            name: 'victim_left_leg_layer_cube',
            parent: 'victim_left_leg',
            from: [0, 0, -2],
            to: [4, 12, 2],
            uv: [0, 48],
            inflate: 0.25
        }
    ];
    const PLAYER_ITEM_GROUPS = [
        {
            name: 'victim_right_item',
            parent: 'victim_right_arm',
            // The vanilla right hand is centered at x=6 (the arm pivot is x=5).
            // Item attachments must use the palm center so third-person item
            // transforms line up with the player's actual held item.
            origin: [6, 12, 0]
        },
        {
            name: 'victim_left_item',
            parent: 'victim_left_arm',
            // Mirror of the right-hand palm center.
            origin: [-6, 12, 0]
        }
    ];

    const COLORS = {
        physical_detectable: 0xffffff,
        physical: 0x4488ff,
        detectable: 0xffff00,
        sensor: 0x00ff00
    };

    const REFRESH_EVENTS = [
        'finish_edit',
        'undo',
        'redo',
        'load_project',
        'setup_project',
        'select_project',
        'reset_project',
        'update_outliner',
        'update_view'
    ];

    let refreshTimer = null;
    let materials = null;
    let settingsItems = [];
    let menuItems = [];
    let playerSkinDialog = null;
    let activePlayerSkinMap = null;
    let activePlayerSkinMaterial = null;
    let skinRequestSerial = 0;
    let defaultSkinAttempted = false;
    let cameraPreviewPanel = null;
    let cameraPreviewFrame = null;
    let disposeCameraPreview = null;
    let resetCameraPreviewModel = null;
    const overlays = new Map();
    const hiddenGroupVisibility = new Map();
    const hiddenCubeVisibility = new Map();
    const playerSkinOverrides = new Map();
    // Viewport-only skin meshes must stay outside Blockbench's element tree.
    // THREE children attached to Cube.mesh can be traversed by project close
    // and format-conversion code, preventing those operations from finishing.
    let playerSkinRoot = null;
    let playerSkinSceneInverse = null;

    function classifyBone(name) {
        if (typeof name !== 'string' || name.length < 3 || name.charAt(0) !== 'h') {
            return null;
        }

        const underscore = name.indexOf('_');
        if (underscore <= 1 || underscore > 5) return null;

        const prefix = name.substring(1, underscore);
        const suffix = name.charAt(underscore - 1);
        if (suffix !== 'b' && suffix !== 's') return null;

        for (let i = 0; i < prefix.length - 1; i++) {
            const flag = prefix.charAt(i);
            if (flag !== 'a' && flag !== 'd') return null;
        }

        return {
            physical: suffix === 'b',
            render: prefix.indexOf('a') >= 0,
            detectable: prefix.indexOf('d') >= 0
        };
    }

    function colorFor(type) {
        if (type.physical && type.detectable) return COLORS.physical_detectable;
        if (type.physical) return COLORS.physical;
        if (type.detectable) return COLORS.detectable;
        return COLORS.sensor;
    }

    function settingValue(id, fallback) {
        return typeof settings !== 'undefined' && settings[id]
            ? settings[id].value
            : fallback;
    }

    function createMaterials() {
        const lineOptions = {
            transparent: true,
            opacity: 1,
            depthTest: false,
            depthWrite: false,
            toneMapped: false,
            fog: false
        };

        return {
            wireframes: new Map(Object.values(COLORS).map(color => [
                color,
                new THREE.LineBasicMaterial(Object.assign({color}, lineOptions))
            ])),
            axes: new THREE.LineBasicMaterial(Object.assign({}, lineOptions, {
                vertexColors: true,
                opacity: 0.5
            })),
            hiddenFaces: new THREE.MeshBasicMaterial({
                visible: false,
                transparent: true,
                opacity: 0,
                depthWrite: false,
                colorWrite: false
            })
        };
    }

    function createAxesGeometry(sourceGeometry) {
        if (!sourceGeometry.boundingBox) sourceGeometry.computeBoundingBox();
        const bounds = sourceGeometry.boundingBox;
        if (!bounds) return null;

        const cx = (bounds.min.x + bounds.max.x) * 0.5;
        const cy = (bounds.min.y + bounds.max.y) * 0.5;
        const cz = (bounds.min.z + bounds.max.z) * 0.5;
        const positions = new Float32Array([
            cx, cy, cz, bounds.max.x, cy, cz,
            cx, cy, cz, cx, bounds.max.y, cz,
            cx, cy, cz, cx, cy, bounds.max.z
        ]);
        const colors = new Float32Array([
            1, 0.392, 0.392, 1, 0.392, 0.392,
            0.392, 1, 0.392, 0.392, 1, 0.392,
            0.392, 0.392, 1, 0.392, 0.392, 1
        ]);

        const geometry = new THREE.BufferGeometry();
        geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
        geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));
        return geometry;
    }

    function disableRaycast(object) {
        object.raycast = function() {};
        object.frustumCulled = false;
        object.renderOrder = 1000;
        return object;
    }

    function createOverlay(cube, type, showPreview, hideAll) {
        if (!cube.mesh) return null;

        const overlay = {
            cube,
            mesh: cube.mesh,
            root: null,
            ownedGeometries: [],
            originalMaterial: null
        };

        if (showPreview && !type.render && settingValue(SETTING_RUNTIME_VISIBILITY, true)) {
            overlay.originalMaterial = cube.mesh.material;
            cube.mesh.material = materials.hiddenFaces;
        }

        if (!showPreview || hideAll) return overlay;
        if (!cube.mesh.geometry || !cube.mesh.parent) return overlay;

        cube.mesh.updateMatrix();

        const root = new THREE.Object3D();
        root.name = PLUGIN_ID + ':' + cube.uuid;
        root.userData[PLUGIN_ID] = true;
        root.visible = cube.visibility !== false;
        root.matrixAutoUpdate = false;
        root.matrix.copy(cube.mesh.matrix);

        const edgesGeometry = new THREE.EdgesGeometry(cube.mesh.geometry);
        overlay.ownedGeometries.push(edgesGeometry);

        const wireframe = disableRaycast(new THREE.LineSegments(
            edgesGeometry,
            materials.wireframes.get(colorFor(type))
        ));
        wireframe.name = root.name + ':wireframe';
        root.add(wireframe);

        if (settingValue(SETTING_AXES, true)) {
            const axesGeometry = createAxesGeometry(cube.mesh.geometry);
            if (axesGeometry) {
                overlay.ownedGeometries.push(axesGeometry);
                const axes = disableRaycast(new THREE.LineSegments(axesGeometry, materials.axes));
                axes.name = root.name + ':axes';
                root.add(axes);
            }
        }

        cube.mesh.parent.add(root);
        overlay.root = root;

        return overlay;
    }

    function removeOverlay(overlay) {
        if (overlay.root && overlay.root.parent) overlay.root.parent.remove(overlay.root);
        overlay.ownedGeometries.forEach(geometry => geometry.dispose());

        if (overlay.originalMaterial !== null
            && overlay.mesh.material === materials.hiddenFaces) {
            overlay.mesh.material = overlay.originalMaterial;
        }

    }

    function clearOverlays() {
        overlays.forEach(removeOverlay);
        overlays.clear();
    }

    function updateCanvasVisibility() {
        if (typeof Canvas !== 'undefined' && Canvas
            && typeof Canvas.updateVisibility === 'function') {
            Canvas.updateVisibility();
        }
    }

    function restoreTrackedVisibility(entries) {
        let changed = false;
        entries.forEach(entry => {
            if (entry.element && entry.element.visibility !== entry.visible) {
                entry.element.visibility = entry.visible;
                changed = true;
            }
        });
        entries.clear();
        return changed;
    }

    function hideMatchingElements(elements, entries, isHitbox) {
        const hitboxElementIds = new Set();
        let changed = false;

        elements.forEach(element => {
            if (!isHitbox(element)) return;

            hitboxElementIds.add(element.uuid);
            const entry = entries.get(element.uuid);
            if (entry) {
                entry.element = element;
            } else {
                entries.set(element.uuid, {
                    element,
                    visible: element.visibility !== false
                });
            }

            if (element.visibility !== false) {
                element.visibility = false;
                changed = true;
            }
        });

        entries.forEach((entry, uuid) => {
            if (hitboxElementIds.has(uuid)) return;
            if (entry.element && entry.element.visibility !== entry.visible) {
                entry.element.visibility = entry.visible;
                changed = true;
            }
            entries.delete(uuid);
        });

        return changed;
    }

    function restoreHiddenHitboxVisibility(updateViewport) {
        const groupsChanged = restoreTrackedVisibility(hiddenGroupVisibility);
        const cubesChanged = restoreTrackedVisibility(hiddenCubeVisibility);
        if ((groupsChanged || cubesChanged) && updateViewport !== false) updateCanvasVisibility();
    }

    function syncHiddenHitboxVisibility(groups, cubes, hideAll) {
        if (!hideAll) {
            restoreHiddenHitboxVisibility();
            return;
        }

        const groupsChanged = hideMatchingElements(
            groups,
            hiddenGroupVisibility,
            group => typeof group.name === 'string' && !!classifyBone(group.name)
        );
        const cubesChanged = hideMatchingElements(
            cubes,
            hiddenCubeVisibility,
            cube => !!cube.parent
                && typeof cube.parent.name === 'string'
                && !!classifyBone(cube.parent.name)
        );

        const changed = groupsChanged || cubesChanged;
        if (changed) updateCanvasVisibility();
    }

    function isInsideGroup(element, groupName) {
        let parent = element && element.parent;
        while (parent) {
            if (parent.name === groupName) return true;
            parent = parent.parent;
        }
        return false;
    }

    function isDescendantOf(element, ancestor) {
        let parent = element && element.parent;
        while (parent) {
            if (parent === ancestor) return true;
            parent = parent.parent;
        }
        return false;
    }

    function getVictimCubes() {
        if (typeof Cube === 'undefined' || !Array.isArray(Cube.all)) return [];
        return Cube.all.filter(cube => isInsideGroup(cube, 'victim_root'));
    }

    function getPlayerSkinScene() {
        if (typeof Canvas === 'undefined' || !Canvas || !Canvas.scene
            || typeof THREE === 'undefined' || !THREE.Group) {
            return null;
        }
        return Canvas.scene;
    }

    function ensurePlayerSkinRoot() {
        const scene = getPlayerSkinScene();
        if (!scene) return null;
        if (!playerSkinRoot) {
            playerSkinRoot = new THREE.Group();
            playerSkinRoot.name = PLUGIN_ID + ':player_skin_root';
            playerSkinRoot.userData[PLUGIN_ID] = true;
            playerSkinRoot.matrixAutoUpdate = false;
            playerSkinRoot.matrix.identity();
            playerSkinRoot.visible = true;
        }
        if (playerSkinRoot.parent !== scene) {
            if (playerSkinRoot.parent) playerSkinRoot.parent.remove(playerSkinRoot);
            scene.add(playerSkinRoot);
        }
        playerSkinRoot.visible = true;
        return playerSkinRoot;
    }

    function syncPlayerSkinTransforms() {
        if (!playerSkinOverrides.size) return;
        const root = ensurePlayerSkinRoot();
        if (!root) return;
        const scene = getPlayerSkinScene();
        if (scene && typeof scene.updateMatrixWorld === 'function') {
            scene.updateMatrixWorld(true);
        }
        let sceneInverse = null;
        if (scene && scene.matrixWorld && typeof THREE.Matrix4 !== 'undefined') {
            if (!playerSkinSceneInverse) playerSkinSceneInverse = new THREE.Matrix4();
            sceneInverse = playerSkinSceneInverse.copy(scene.matrixWorld).invert();
        }
        playerSkinOverrides.forEach(entry => {
            const cubeMesh = entry.cube && entry.cube.mesh === entry.mesh
                ? entry.mesh : null;
            if (!cubeMesh || !entry.skinMesh) return;
            if (typeof cubeMesh.updateMatrixWorld === 'function') {
                cubeMesh.updateMatrixWorld(true);
            }
            if (cubeMesh.matrixWorld && entry.skinMesh.matrix) {
                // playerSkinRoot is a child of Canvas.scene. Convert the cube
                // world matrix into scene-local coordinates so Canvas.scene's
                // own preview rotation is not applied twice.
                if (sceneInverse) {
                    entry.skinMesh.matrix.multiplyMatrices(sceneInverse, cubeMesh.matrixWorld);
                } else {
                    entry.skinMesh.matrix.copy(cubeMesh.matrixWorld);
                }
                entry.skinMesh.matrixWorldNeedsUpdate = true;
            }
            entry.skinMesh.visible = entry.cube.visibility !== false
                && cubeMesh.visible !== false;
        });
        if (scene && typeof scene.updateMatrixWorld === 'function') {
            scene.updateMatrixWorld(true);
        }
    }

    function clearPlayerSkinOverrides() {
        playerSkinOverrides.forEach(entry => {
            if (entry.skinMesh && entry.skinMesh.parent) {
                entry.skinMesh.parent.remove(entry.skinMesh);
            }
            if (entry.skinGeometry) entry.skinGeometry.dispose();
            if (entry.cube && entry.cube.mesh === entry.mesh) {
                entry.cube.mesh.material = entry.originalMaterial;
            }
        });
        playerSkinOverrides.clear();
    }

    function removePlayerSkinRoot() {
        clearPlayerSkinOverrides();
        if (playerSkinRoot && playerSkinRoot.parent) {
            playerSkinRoot.parent.remove(playerSkinRoot);
        }
        playerSkinRoot = null;
        playerSkinSceneInverse = null;
    }

    /**
     * Build the standard Minecraft 64x64 box-UV layout on a cloned mesh.
     * The clone preserves the source cube's inflated geometry for second
     * layers. It is viewport-only; no Cube faces, Texture resources, or
     * exported model data are changed.
     */
    function createPlayerSkinGeometry(cube) {
        const source = cube && cube.mesh && cube.mesh.geometry;
        if (!source || !source.attributes || !source.attributes.position) return null;
        const geometry = source.clone();
        if (geometry.attributes.position.count < 24) {
            geometry.dispose();
            return null;
        }

        const size = typeof cube.size === 'function' ? cube.size() : [0, 0, 0];
        const offset = Array.isArray(cube.uv_offset) ? cube.uv_offset : [0, 0];
        const uvFaces = {
            east: [0, size[2], size[2], size[1]],
            west: [size[2] + size[0], size[2], size[2], size[1]],
            up: [size[2] + size[0], size[2], -size[0], -size[2]],
            down: [size[2] + size[0] * 2, 0, -size[0], size[2]],
            south: [size[2] * 2 + size[0], size[2], size[0], size[1]],
            north: [size[2], size[2], size[0], size[1]]
        };

        if (cube.mirror_uv) {
            Object.keys(uvFaces).forEach(face => {
                uvFaces[face][0] += uvFaces[face][2];
                uvFaces[face][2] *= -1;
            });
            const east = uvFaces.east;
            uvFaces.east = uvFaces.west;
            uvFaces.west = east;
        }

        const uv = new Float32Array(24 * 2);
        ['east', 'west', 'up', 'down', 'south', 'north'].forEach((face, faceIndex) => {
            const entry = uvFaces[face];
            const u0 = (entry[0] + offset[0]) / 64;
            const v0 = entry[1] + offset[1];
            const u1 = (entry[0] + entry[2] + offset[0]) / 64;
            const v1 = entry[1] + entry[3] + offset[1];
            uv.set([
                u0, 1 - v0 / 64,
                u1, 1 - v0 / 64,
                u0, 1 - v1 / 64,
                u1, 1 - v1 / 64
            ], faceIndex * 8);
        });

        const indices = [];
        for (let face = 0; face < 6; face++) {
            const vertex = face * 4;
            indices.push(vertex, vertex + 2, vertex + 1,
                vertex + 2, vertex + 3, vertex + 1);
        }
        geometry.setIndex(indices);
        geometry.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
        geometry.clearGroups();
        geometry.addGroup(0, indices.length, 0);
        geometry.attributes.uv.needsUpdate = true;
        return geometry;
    }

    /**
     * Applies the downloaded skin as a viewport-only mesh overlay. The
     * Blockbench Cube faces and Texture list are deliberately untouched.
     * Only cubes below victim_root are affected.
     */
    function applyPlayerSkinOverrides() {
        clearPlayerSkinOverrides();
        if (!activePlayerSkinMaterial || !materials || !materials.hiddenFaces) return;
        const root = ensurePlayerSkinRoot();
        if (!root) return;

        getVictimCubes().forEach(cube => {
            if (!cube.mesh || !cube.mesh.geometry) return;
            const skinGeometry = createPlayerSkinGeometry(cube);
            if (!skinGeometry) return;
            const skinMesh = new THREE.Mesh(skinGeometry, activePlayerSkinMaterial);
            skinMesh.name = PLUGIN_ID + ':player_skin:' + cube.uuid;
            skinMesh.userData[PLUGIN_ID] = true;
            skinMesh.no_export = true;
            skinMesh.frustumCulled = false;
            skinMesh.raycast = function() {};
            skinMesh.matrixAutoUpdate = false;
            root.add(skinMesh);
            playerSkinOverrides.set(cube.uuid, {
                cube,
                mesh: cube.mesh,
                originalMaterial: cube.mesh.material,
                skinMesh,
                skinGeometry
            });
            cube.mesh.material = materials.hiddenFaces;
        });
        syncPlayerSkinTransforms();
    }

    function disposePlayerSkin() {
        clearPlayerSkinOverrides();
        if (activePlayerSkinMaterial) activePlayerSkinMaterial.dispose();
        if (activePlayerSkinMap) activePlayerSkinMap.dispose();
        activePlayerSkinMaterial = null;
        activePlayerSkinMap = null;
    }

    function loadRenderSkin(dataUrl) {
        return new Promise((resolve, reject) => {
            if (typeof THREE === 'undefined' || !THREE.TextureLoader) {
                reject(new Error(text('cnpc_immersiveboss.errors.viewport_texture', 'This Blockbench version cannot create viewport skin materials.')));
                return;
            }
            const loader = new THREE.TextureLoader();
            loader.load(dataUrl, texture => {
                texture.magFilter = THREE.NearestFilter;
                texture.minFilter = THREE.NearestFilter;
                texture.wrapS = THREE.ClampToEdgeWrapping;
                texture.wrapT = THREE.ClampToEdgeWrapping;
                texture.needsUpdate = true;
                resolve(texture);
            }, undefined, () => reject(new Error(text('cnpc_immersiveboss.errors.skin_image', 'Failed to read the player skin image.'))));
        });
    }

    async function fetchJson(url) {
        if (typeof fetch !== 'function') {
            throw new Error(text('cnpc_immersiveboss.errors.network', 'This Blockbench version cannot make network requests. Upgrade to 4.10 or newer.'));
        }
        const response = await fetch(url, {
            headers: {Accept: 'application/json'}
        });
        if (response.status === 204) return null;
        if (!response.ok) {
            throw new Error('HTTP ' + response.status + ' (' + response.statusText + ')');
        }
        return response.json();
    }

    function decodeBase64Json(value) {
        if (typeof Buffer !== 'undefined' && Buffer.from) {
            return JSON.parse(Buffer.from(value, 'base64').toString('utf8'));
        }
        if (typeof atob !== 'function') throw new Error(text('cnpc_immersiveboss.errors.decode_profile', 'Could not decode Mojang skin profile data.'));
        const binary = atob(value);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const text = typeof TextDecoder !== 'undefined'
            ? new TextDecoder().decode(bytes)
            : decodeURIComponent(escape(binary));
        return JSON.parse(text);
    }

    async function blobToDataUrl(blob) {
        if (typeof FileReader !== 'undefined') {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result);
                reader.onerror = () => reject(new Error(text('cnpc_immersiveboss.errors.skin_image', 'Failed to read the player skin image.')));
                reader.readAsDataURL(blob);
            });
        }
        if (typeof Buffer !== 'undefined' && Buffer.from) {
            return 'data:image/png;base64,' + Buffer.from(
                new Uint8Array(await blob.arrayBuffer())
            ).toString('base64');
        }
        throw new Error(text('cnpc_immersiveboss.errors.image_data', 'This Blockbench version cannot convert image data.'));
    }

    async function resolveOfficialSkin(username) {
        const profile = await fetchJson(
            'https://api.mojang.com/users/profiles/minecraft/' + encodeURIComponent(username)
        );
        if (!profile || !profile.id) {
            throw new Error(text('cnpc_immersiveboss.errors.player_not_found', 'Official player not found: %0', [username]));
        }

        const session = await fetchJson(
            'https://sessionserver.mojang.com/session/minecraft/profile/' + profile.id
        );
        const property = session && Array.isArray(session.properties)
            ? session.properties.find(item => item && item.name === 'textures')
            : null;
        if (!property || !property.value) {
            throw new Error(text('cnpc_immersiveboss.errors.profile_missing', 'Mojang did not return skin profile data for this player.'));
        }

        const textureProfile = decodeBase64Json(property.value);
        const skin = textureProfile && textureProfile.textures
            && textureProfile.textures.SKIN;
        if (!skin || !skin.url) {
            throw new Error(text('cnpc_immersiveboss.errors.skin_missing', 'This player does not have an available official skin.'));
        }

        // Mojang's profile payload may still return an http URL; use the
        // canonical HTTPS endpoint so Blockbench's web view accepts it.
        const skinUrl = skin.url.replace(/^http:\/\//i, 'https://');
        const imageResponse = await fetch(skinUrl);
        if (!imageResponse.ok) {
            throw new Error(text('cnpc_immersiveboss.errors.skin_download', 'Failed to download the player skin image (HTTP %0).', [imageResponse.status]));
        }
        return {
            name: profile.name || username,
            dataUrl: await blobToDataUrl(await imageResponse.blob())
        };
    }

    async function loadOfficialPlayerSkin(username, silent) {
        const cubes = getVictimCubes();
        if (!cubes.length) {
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.no_victim', 'This project has no victim_root player model. Run Create IB Player Rig first.')
            });
            return;
        }

        const serial = ++skinRequestSerial;
        const requestProject = typeof Project !== 'undefined' ? Project : null;
        Blockbench.showQuickMessage(text('cnpc_immersiveboss.messages.loading_skin', 'Fetching player skin from Mojang…'));
        try {
            const result = await resolveOfficialSkin(username);
            if (serial !== skinRequestSerial
                || (typeof Project !== 'undefined' ? Project : null) !== requestProject) return;

            const skinMap = await loadRenderSkin(result.dataUrl);
            if (serial !== skinRequestSerial
                || (typeof Project !== 'undefined' ? Project : null) !== requestProject) {
                skinMap.dispose();
                return;
            }

            const skinMaterial = new THREE.MeshBasicMaterial({
                map: skinMap,
                transparent: true,
                alphaTest: 0.01,
                side: THREE.DoubleSide
            });
            disposePlayerSkin();
            activePlayerSkinMap = skinMap;
            activePlayerSkinMaterial = skinMaterial;
            if ((typeof Project !== 'undefined' ? Project : null) !== requestProject) {
                disposePlayerSkin();
                return;
            }
            Canvas.updateAll();
            applyPlayerSkinOverrides();
            Blockbench.showQuickMessage(text('cnpc_immersiveboss.messages.skin_applied', 'Applied %0\'s official skin to victim_root.', [result.name]));
        } catch (error) {
            if (serial !== skinRequestSerial) return;
            if (silent) return;
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.skin_failed', 'Failed to fetch the official player skin: %0', [error && error.message
                    ? error.message : String(error)])
            });
        }
    }

    function loadDefaultPlayerSkinIfAvailable() {
        if (defaultSkinAttempted || activePlayerSkinMaterial) return;
        if (!getVictimCubes().length) return;

        defaultSkinAttempted = true;
        loadOfficialPlayerSkin(DEFAULT_PLAYER_SKIN, true).catch(() => {
            // The explicit skin action still lets users retry with another
            // name when Mojang is unavailable during plugin startup.
        });
    }

    function createRigGroup(name, origin, parent, rotation) {
        const group = new Group({
            name,
            origin,
            rotation: rotation || [0, 0, 0]
        }).init();
        if (parent) group.addTo(parent);
        return group;
    }

    function createPlayerCube(name, from, to, uvOffset, parent, inflate) {
        const cube = new Cube({
            name,
            from,
            to,
            box_uv: true,
            uv_offset: uvOffset,
            inflate: inflate || 0,
            autouv: 0
        }).init();
        cube.addTo(parent);
        return cube;
    }

    function findPlayerRigGroup(root, name) {
        if (typeof Group === 'undefined' || !Array.isArray(Group.all)) return null;
        return Group.all.find(group => group.name === name
            && isDescendantOf(group, root)) || null;
    }

    function findPlayerRigCube(root, name) {
        if (typeof Cube === 'undefined' || !Array.isArray(Cube.all)) return null;
        return Cube.all.find(cube => cube.name === name
            && isDescendantOf(cube, root)) || null;
    }

    function playerCubeInflate(cube) {
        const inflate = Number(cube && cube.inflate);
        return Number.isFinite(inflate) ? inflate : 0;
    }

    function playerCubeUvOffset(cube) {
        return cube && Array.isArray(cube.uv_offset) ? cube.uv_offset : [0, 0];
    }

    function findPlayerRigCubeForSpec(root, spec) {
        const named = findPlayerRigCube(root, spec.name);
        if (named) return named;
        if (typeof Cube === 'undefined' || !Array.isArray(Cube.all)) return null;
        const parent = findPlayerRigGroup(root, spec.parent);
        if (!parent) return null;
        const expectedInflate = Number(spec.inflate) || 0;
        return Cube.all.find(cube => cube.parent === parent
            && playerCubeInflate(cube) === expectedInflate
            && coordinatesEqual(playerCubeUvOffset(cube), spec.uv)) || null;
    }

    function findPlayerRigRoot() {
        if (typeof Group === 'undefined' || !Array.isArray(Group.all)) return null;
        const candidates = Group.all.filter(group => group.name === 'victim_root');
        if (candidates.length < 2) return candidates[0] || null;
        return candidates.sort((left, right) => {
            const score = root => {
                const selected = root.selected || root.primary_selected
                    || (Array.isArray(Group.multi_selected) && Group.multi_selected.includes(root));
                const parts = PLAYER_RIG_GROUPS.filter(spec => findPlayerRigGroup(root, spec.name)).length;
                return (selected ? 100 : 0) + parts;
            };
            return score(right) - score(left);
        })[0];
    }

    function resolveCameraType() {
        return typeof CameraElement !== 'undefined' && CameraElement
            ? CameraElement
            : (typeof OutlinerElement !== 'undefined'
                && OutlinerElement.types ? OutlinerElement.types.camera : null);
    }

    function findCameraGroup(name, root) {
        const local = root && findPlayerRigGroup(root, name);
        if (local) return local;
        // A camera bone is allowed to be re-parented anywhere in the model.
        // Fall back to the globally named standard bone so the completion
        // action does not create a duplicate after artists move it.
        if (typeof Group === 'undefined' || !Array.isArray(Group.all)) return null;
        return Group.all.find(group => group.name === name) || null;
    }

    function findCameraElement(CameraType, name, root) {
        if (!CameraType || !Array.isArray(CameraType.all)) return null;
        const local = CameraType.all.find(camera => camera.name === name
            && (!root || isDescendantOf(camera, root)));
        return local || CameraType.all.find(camera => camera.name === name) || null;
    }

    function findCameraForGroup(CameraType, group, name) {
        if (!CameraType || !Array.isArray(CameraType.all) || !group) return null;
        return CameraType.all.find(camera =>
            (camera.parent === group || isDescendantOf(camera, group))
            && (!name || camera.name === name)) || null;
    }

    function createRigCamera(CameraType, spec, group) {
        let camera = null;
        try {
            camera = new CameraType({
                name: spec.camera,
                position: [0, 0, 0],
                rotation: [0, 0, 0],
                fov: 70
            }).init();
        } catch (error) {
            try {
                camera = new CameraType().init();
                camera.name = spec.camera;
                camera.position = [0, 0, 0];
                camera.rotation = [0, 0, 0];
                camera.fov = 70;
            } catch (fallbackError) {
                return null;
            }
        }
        if (!camera) return null;
        camera.addTo(group);
        // CameraElement positions are absolute model-space coordinates in
        // Blockbench bone-rig projects, matching the Cameras plugin's own
        // Add Camera action. The bone still carries the runtime transform.
        if (typeof Format !== 'undefined' && Format && Format.bone_rig) {
            const cameraPosition = group.origin.slice();
            if (typeof camera.extend === 'function') {
                camera.extend({position: cameraPosition});
            } else {
                camera.position = cameraPosition;
            }
        }
        if ('export' in camera) camera.export = false;
        return camera;
    }

    function offsetCoordinates(coordinates, offset) {
        return coordinates.map((value, index) => Number(value) + offset[index]);
    }

    function playerRigOrigin(root) {
        const offsets = [];
        PLAYER_BASE_CUBES.concat(PLAYER_SKIN_LAYERS).forEach(spec => {
            const cube = findPlayerRigCubeForSpec(root, spec);
            if (!cube || !Array.isArray(cube.from)) return;
            offsets.push(cube.from.map((value, index) => Number(value) - spec.from[index]));
        });
        if (offsets.length) {
            return [0, 1, 2].map(axis => {
                const values = offsets.map(offset => offset[axis]).sort((left, right) => left - right);
                const middle = Math.floor(values.length / 2);
                return values.length % 2 ? values[middle] : (values[middle - 1] + values[middle]) / 2;
            });
        }
        if (!root || !Array.isArray(root.origin) || root.origin.length !== 3) return [0, 0, 0];
        return root.origin.map(value => Number.isFinite(Number(value)) ? Number(value) : 0);
    }

    function playerGroupOffset(root, groupName, rigOrigin) {
        const group = findPlayerRigGroup(root, groupName);
        const spec = PLAYER_RIG_GROUPS.find(entry => entry.name === groupName);
        if (!group || !spec || !Array.isArray(group.origin)) return rigOrigin;
        return group.origin.map((value, index) => Number(value) - spec.origin[index]);
    }

    function ensurePlayerBaseRig(root, created, rigOrigin) {
        PLAYER_RIG_GROUPS.forEach(spec => {
            if (findPlayerRigGroup(root, spec.name)) return;
            created.push(createRigGroup(spec.name, offsetCoordinates(spec.origin, rigOrigin), root));
        });

        PLAYER_BASE_CUBES.forEach(spec => {
            if (findPlayerRigCubeForSpec(root, spec)) return;
            const parent = findPlayerRigGroup(root, spec.parent);
            if (!parent) return;
            const layerSpec = PLAYER_SKIN_LAYERS.find(layer => layer.parent === spec.parent);
            const layer = layerSpec && findPlayerRigCubeForSpec(root, layerSpec);
            const offset = layer && Array.isArray(layer.from)
                ? layer.from.map((value, index) => Number(value) - layerSpec.from[index])
                : playerGroupOffset(root, spec.parent, rigOrigin);
            created.push(createPlayerCube(
                spec.name,
                offsetCoordinates(spec.from, offset),
                offsetCoordinates(spec.to, offset),
                spec.uv,
                parent
            ));
        });
    }

    function ensurePlayerCameras(root, CameraType, created, rigOrigin) {
        if (!root || !CameraType) return {changed: false, failed: false};
        const head = findPlayerRigGroup(root, 'victim_head') || root;
        const cameraOffset = playerGroupOffset(root, 'victim_head', rigOrigin);
        let changed = false;
        let failed = false;

        PLAYER_CAMERA_SPECS.forEach(spec => {
            let group = findCameraGroup(spec.bone, root);
            if (!group) {
                group = createRigGroup(
                    spec.bone,
                    offsetCoordinates(spec.origin, cameraOffset),
                    head,
                    spec.rotation
                );
                created.push(group);
                changed = true;
            }

            let camera = findCameraForGroup(CameraType, group, spec.camera);
            if (!camera) camera = findCameraElement(CameraType, spec.camera, root);
            if (!camera) {
                camera = createRigCamera(CameraType, spec, group);
                if (!camera) {
                    failed = true;
                    return;
                }
                created.push(camera);
                changed = true;
            }
        });

        return {changed, failed};
    }

    function registerCameraPreviewPanel() {
        if (cameraPreviewPanel || typeof Panel === 'undefined'
            || typeof document === 'undefined' || typeof THREE === 'undefined') return;

        let selectedSpec = PLAYER_CAMERA_SPECS[0];
        const wrapper = document.createElement('div');
        wrapper.style.display = 'flex';
        wrapper.style.flexDirection = 'column';
        wrapper.style.height = '100%';
        wrapper.style.minHeight = '0';
        wrapper.style.minWidth = '0';
        wrapper.style.boxSizing = 'border-box';
        wrapper.style.overflow = 'hidden';
        wrapper.style.gap = '4px';
        wrapper.style.padding = '4px';

        const select = document.createElement('select');
        select.title = text('cnpc_immersiveboss.camera_preview.select', 'Preview camera view');
        select.style.width = '100%';
        select.style.minWidth = '0';
        select.style.flex = '0 0 auto';
        PLAYER_CAMERA_SPECS.forEach(spec => {
            const option = document.createElement('option');
            option.value = spec.bone;
            option.innerText = text(spec.label, spec.fallbackLabel);
            select.appendChild(option);
        });
        select.onchange = () => {
            selectedSpec = PLAYER_CAMERA_SPECS.find(spec => spec.bone === select.value)
                || PLAYER_CAMERA_SPECS[0];
        };
        wrapper.appendChild(select);

        const viewport = document.createElement('div');
        viewport.style.position = 'relative';
        viewport.style.flex = '1 1 auto';
        viewport.style.minHeight = '0';
        viewport.style.minWidth = '0';
        viewport.style.overflow = 'hidden';
        viewport.style.background = 'var(--color-back)';
        viewport.style.outline = '1px solid var(--color-border)';
        wrapper.appendChild(viewport);

        const crosshair = document.createElement('span');
        crosshair.innerText = '+';
        crosshair.style.position = 'absolute';
        crosshair.style.left = '50%';
        crosshair.style.top = '50%';
        crosshair.style.transform = 'translate(-50%, -50%)';
        crosshair.style.zIndex = '1';
        crosshair.style.pointerEvents = 'none';
        viewport.appendChild(crosshair);

        const missing = document.createElement('div');
        missing.style.position = 'absolute';
        missing.style.inset = '0';
        missing.style.display = 'none';
        missing.style.alignItems = 'center';
        missing.style.justifyContent = 'center';
        missing.style.padding = '12px';
        missing.style.textAlign = 'center';
        missing.style.color = 'var(--color-subtle_text)';
        missing.style.pointerEvents = 'none';
        viewport.appendChild(missing);

        cameraPreviewPanel = new Panel(CAMERA_PREVIEW_PANEL, {
            name: text('cnpc_immersiveboss.camera_preview.name', 'Camera View Preview'),
            icon: 'videocam',
            condition: {modes: ['animate']},
            growable: true,
            resizable: true,
            min_height: 190,
            default_position: {slot: 'left_bar', height: 300, width: 340}
        });
        cameraPreviewPanel.node.appendChild(wrapper);

        let renderer = null;
        const scene = new THREE.Scene();
        const model = new THREE.Object3D();
        scene.add(model);
        const camera = new THREE.PerspectiveCamera(70, 16 / 9, 0.05, 30000);
        const inverseRoot = new THREE.Matrix4();
        const localCameraMatrix = new THREE.Matrix4();
        const cameraScale = new THREE.Vector3();
        const copies = new Map();
        let sourceRoot = null;
        let lights = null;
        let width = 0;
        let height = 0;
        let pixelRatio = 0;

        function clearModel() {
            model.clear();
            copies.clear();
            sourceRoot = null;
            if (lights) scene.remove(lights);
            lights = null;
        }
        resetCameraPreviewModel = clearModel;

        function updateLayout(force) {
            const nextWidth = Math.max(0, Math.floor(viewport.clientWidth));
            const nextHeight = Math.max(0, Math.floor(viewport.clientHeight));
            const nextPixelRatio = Math.min(Number(window.devicePixelRatio) || 1, 2);
            if (force || nextWidth !== width || nextHeight !== height || nextPixelRatio !== pixelRatio) {
                width = nextWidth;
                height = nextHeight;
                pixelRatio = nextPixelRatio;
                if (renderer) {
                    renderer.setPixelRatio(pixelRatio);
                    renderer.setSize(width, height, false);
                }
                camera.aspect = height > 0 ? width / height : 1;
                camera.updateProjectionMatrix();
            }
            return width > 0 && height > 0;
        }

        const resizeObserver = typeof ResizeObserver !== 'undefined'
            ? new ResizeObserver(() => updateLayout(false)) : null;
        if (resizeObserver) resizeObserver.observe(wrapper);

        function syncSceneCopies() {
            if (sourceRoot !== Project.model_3d) {
                clearModel();
                sourceRoot = Project.model_3d;
                lights = Canvas.scene.children.find(child => child.name === 'lights');
                lights = lights && typeof lights.clone === 'function' ? lights.clone(true) : null;
                if (lights) scene.add(lights);
            }

            sourceRoot.updateMatrixWorld(true);
            inverseRoot.copy(sourceRoot.matrixWorld).invert();
            const active = new Set();
            const elements = typeof Outliner !== 'undefined' && Array.isArray(Outliner.elements)
                ? Outliner.elements : [];
            elements.forEach(element => {
                const source = element.mesh;
                if (element.type === 'camera' || !(source instanceof THREE.Mesh)
                    || source.userData && source.userData[PLUGIN_ID]) return;
                active.add(source);
                let copy = copies.get(source);
                if (!copy) {
                    copy = new THREE.Mesh(source.geometry, source.material);
                    copy.matrixAutoUpdate = false;
                    copy.frustumCulled = false;
                    copies.set(source, copy);
                    model.add(copy);
                }
                copy.geometry = source.geometry;
                copy.material = source.material;
                copy.renderOrder = source.renderOrder;
                copy.visible = element.visibility !== false;
                for (let ancestor = source; ancestor && ancestor !== sourceRoot; ancestor = ancestor.parent) {
                    if (ancestor.visible === false) copy.visible = false;
                }
                copy.matrix.multiplyMatrices(inverseRoot, source.matrixWorld);
                copy.matrixWorldNeedsUpdate = true;
            });
            playerSkinOverrides.forEach(entry => {
                const source = entry.skinMesh;
                if (!(source instanceof THREE.Mesh) || source.visible === false) return;
                source.updateMatrixWorld(true);
                active.add(source);
                let copy = copies.get(source);
                if (!copy) {
                    copy = new THREE.Mesh(source.geometry, source.material);
                    copy.matrixAutoUpdate = false;
                    copy.frustumCulled = false;
                    copies.set(source, copy);
                    model.add(copy);
                }
                copy.geometry = source.geometry;
                copy.material = source.material;
                copy.visible = true;
                copy.matrix.multiplyMatrices(inverseRoot, source.matrixWorld);
                copy.matrixWorldNeedsUpdate = true;
            });
            copies.forEach((copy, source) => {
                if (!active.has(source)) {
                    model.remove(copy);
                    copies.delete(source);
                }
            });
        }

        function syncCamera() {
            const group = findCameraGroup(selectedSpec.bone, null);
            if (!group || !group.mesh) {
                missing.innerText = text('cnpc_immersiveboss.camera_preview.missing',
                    'Camera bone %0 was not found. Run Create IB Player Rig first.', [selectedSpec.bone]);
                missing.style.display = 'flex';
                crosshair.style.display = 'none';
                return false;
            }
            missing.style.display = 'none';
            crosshair.style.display = '';

            const CameraType = resolveCameraType();
            const cameraElement = findCameraForGroup(CameraType, group, selectedSpec.camera)
                || findCameraElement(CameraType, selectedSpec.camera, null);
            const cameraMesh = cameraElement && cameraElement.mesh ? cameraElement.mesh : group.mesh;
            cameraMesh.updateMatrixWorld(true);
            localCameraMatrix.multiplyMatrices(inverseRoot, cameraMesh.matrixWorld);
            localCameraMatrix.decompose(camera.position, camera.quaternion, cameraScale);
            camera.fov = Number(cameraMesh.fov || (cameraElement && cameraElement.fov)) || 70;
            camera.aspect = height > 0 ? width / height : 1;
            camera.updateProjectionMatrix();
            return true;
        }

        function render() {
            cameraPreviewFrame = requestAnimationFrame(render);
            if (!cameraPreviewPanel || !cameraPreviewPanel.node.isConnected
                || !cameraPreviewPanel.isVisible() || !Project || !Project.model_3d
                || typeof Modes === 'undefined' || !Modes.selected || Modes.selected.id !== 'animate') return;
            if (!updateLayout(false)) return;
            if (!renderer) {
                renderer = new THREE.WebGLRenderer({alpha: true, antialias: true});
                renderer.domElement.style.width = '100%';
                renderer.domElement.style.height = '100%';
                renderer.domElement.style.display = 'block';
                renderer.domElement.style.pointerEvents = 'none';
                viewport.insertBefore(renderer.domElement, crosshair);
                updateLayout(true);
            }
            if (sourceRoot && sourceRoot !== Project.model_3d) clearModel();
            syncPlayerSkinTransforms();
            syncSceneCopies();
            if (!syncCamera()) return;
            if (typeof Preview !== 'undefined' && Preview.selected && Preview.selected.renderer) {
                renderer.toneMapping = Preview.selected.renderer.toneMapping;
                renderer.toneMappingExposure = Preview.selected.renderer.toneMappingExposure;
            }
            renderer.render(scene, camera);
        }

        disposeCameraPreview = () => {
            if (resizeObserver) resizeObserver.disconnect();
            clearModel();
            resetCameraPreviewModel = null;
            scene.clear();
            if (renderer) {
                renderer.dispose();
                renderer.forceContextLoss();
                renderer.domElement.remove();
                renderer = null;
            }
        };
        cameraPreviewFrame = requestAnimationFrame(render);
    }

    function disposeCameraPreviewPanel() {
        if (cameraPreviewFrame !== null) cancelAnimationFrame(cameraPreviewFrame);
        cameraPreviewFrame = null;
        if (disposeCameraPreview) disposeCameraPreview();
        disposeCameraPreview = null;
        resetCameraPreviewModel = null;
        if (cameraPreviewPanel) cameraPreviewPanel.delete();
        cameraPreviewPanel = null;
    }

    function coordinatesEqual(actual, expected) {
        return Array.isArray(actual)
            && actual.length === expected.length
            && actual.every((value, index) => Number(value) === expected[index]);
    }

    /**
     * Older plugin versions placed the arm bones on the opposite Blockbench
     * side. Migrate only untouched generated arms so custom rigs remain safe.
     */
    function migrateLegacyPlayerArmLayout(root, rigOrigin) {
        const specs = [
            {
                groupName: 'victim_right_arm',
                cubeName: 'victim_right_arm_cube',
                layerName: 'victim_right_arm_layer_cube',
                oldOrigin: [-5, 22, 0],
                newOrigin: [5, 22, 0],
                oldFrom: [-8, 12, -2],
                oldTo: [-4, 24, 2],
                newFrom: [4, 12, -2],
                newTo: [8, 24, 2]
            },
            {
                groupName: 'victim_left_arm',
                cubeName: 'victim_left_arm_cube',
                layerName: 'victim_left_arm_layer_cube',
                oldOrigin: [5, 22, 0],
                newOrigin: [-5, 22, 0],
                oldFrom: [4, 12, -2],
                oldTo: [8, 24, 2],
                newFrom: [-8, 12, -2],
                newTo: [-4, 24, 2]
            }
        ];
        const updates = [];

        specs.forEach(spec => {
            const group = findPlayerRigGroup(root, spec.groupName);
            const cube = findPlayerRigCube(root, spec.cubeName);
            const layer = findPlayerRigCube(root, spec.layerName);
            const oldOrigin = offsetCoordinates(spec.oldOrigin, rigOrigin);
            const newOrigin = offsetCoordinates(spec.newOrigin, rigOrigin);
            const oldFrom = offsetCoordinates(spec.oldFrom, rigOrigin);
            const oldTo = offsetCoordinates(spec.oldTo, rigOrigin);
            const newFrom = offsetCoordinates(spec.newFrom, rigOrigin);
            const newTo = offsetCoordinates(spec.newTo, rigOrigin);
            if (!group || !cube || !coordinatesEqual(group.origin, oldOrigin)
                || !coordinatesEqual(cube.from, oldFrom)
                || !coordinatesEqual(cube.to, oldTo)) {
                return;
            }
            // A customized layer means the arm is no longer an untouched
            // generated arm, so leave the entire arm in place.
            if (layer && (!coordinatesEqual(layer.from, oldFrom)
                || !coordinatesEqual(layer.to, oldTo))) {
                return;
            }
            updates.push({group, cube, layer, newOrigin, newFrom, newTo});
        });

        if (!updates.length) return false;
        updates.forEach(update => {
            update.group.origin = update.newOrigin;
            update.cube.from = update.newFrom;
            update.cube.to = update.newTo;
            if (update.layer) {
                update.layer.from = update.newFrom.slice();
                update.layer.to = update.newTo.slice();
            }
        });
        return true;
    }

    function ensurePlayerSkinLayers(root, created, rigOrigin) {
        PLAYER_SKIN_LAYERS.forEach(layer => {
            if (findPlayerRigCubeForSpec(root, layer)) return;
            const parent = findPlayerRigGroup(root, layer.parent);
            if (!parent) return;
            const baseSpec = PLAYER_BASE_CUBES.find(spec => spec.parent === layer.parent);
            const base = baseSpec && findPlayerRigCubeForSpec(root, baseSpec);
            const offset = base && Array.isArray(base.from)
                ? base.from.map((value, index) => Number(value) - baseSpec.from[index])
                : playerGroupOffset(root, layer.parent, rigOrigin);
            created.push(createPlayerCube(
                layer.name,
                offsetCoordinates(layer.from, offset),
                offsetCoordinates(layer.to, offset),
                layer.uv,
                parent,
                layer.inflate
            ));
        });
    }

    /**
     * Add hand-item attachment bones introduced after the first rig release.
     * Existing groups are intentionally untouched so custom transforms and
     * animation tracks survive when the helper is run again.
     */
    function ensurePlayerItemGroups(root, created, rigOrigin) {
        let changed = false;
        PLAYER_ITEM_GROUPS.forEach(spec => {
            if (findPlayerRigGroup(root, spec.name)) return;
            const parent = findPlayerRigGroup(root, spec.parent);
            if (!parent) return;
            const offset = playerGroupOffset(root, spec.parent, rigOrigin);
            created.push(createRigGroup(spec.name, offsetCoordinates(spec.origin, offset), parent));
            changed = true;
        });
        return changed;
    }

    /**
     * Creates the standard 64x64 Minecraft player puppet used by throw
     * animations. The cubes use the vanilla player skin atlas layout, so the
     * runtime can replace the material with the grabbed player's skin.
     */
    function createPlayerRig() {
        if (typeof Project === 'undefined' || !Project
            || typeof Group === 'undefined' || typeof Cube === 'undefined') {
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.open_model', 'Open a GeckoLib/Bedrock model project first.')
            });
            return;
        }

        const CameraType = resolveCameraType();
        if (!CameraType) {
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.camera_plugin', 'Blockbench does not provide Camera components. Enable the Cameras plugin first.')
            });
            return;
        }

        const existing = findPlayerRigRoot();
        const existingElements = existing ? [
            ...Group.all.filter(group => group === existing || isDescendantOf(group, existing)),
            ...Cube.all.filter(cube => isDescendantOf(cube, existing))
        ] : [];
        const created = [];
        Undo.initEdit({outliner: true, elements: existingElements, selection: true});
        const root = existing || createRigGroup('victim_root', [0, 0, 0]);
        if (!existing) created.push(root);
        const rigOrigin = playerRigOrigin(root);

        const migratedLegacyArms = migrateLegacyPlayerArmLayout(root, rigOrigin);
        ensurePlayerBaseRig(root, created, rigOrigin);
        ensurePlayerSkinLayers(root, created, rigOrigin);
        ensurePlayerItemGroups(root, created, rigOrigin);
        const cameraCompletion = ensurePlayerCameras(root, CameraType, created, rigOrigin);
        if (cameraCompletion.failed) {
            if (typeof Undo.cancelEdit === 'function') Undo.cancelEdit(true);
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.camera_failed', 'Unable to create throw cameras. Check the Blockbench Cameras plugin.')
            });
            return;
        }

        if (existing && !migratedLegacyArms && !created.length) {
            if (typeof Undo.cancelEdit === 'function') Undo.cancelEdit();
            root.select();
            loadDefaultPlayerSkinIfAvailable();
            Blockbench.showMessageBox({
                title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                message: text('cnpc_immersiveboss.messages.rig_exists', 'This project already contains a complete victim_root; nothing was created.')
            });
            return;
        }

        if (!(Project.texture_width > 0) || !(Project.texture_height > 0)) {
            Project.texture_width = 64;
            Project.texture_height = 64;
        }

        Canvas.updateAll();
        root.select();
        Undo.finishEdit(existing
            ? text('cnpc_immersiveboss.undo.complete_rig', 'Complete missing IB player rig parts')
            : text('cnpc_immersiveboss.undo.create_rig', 'Create IB Player Rig'));
        loadDefaultPlayerSkinIfAvailable();
        if (migratedLegacyArms) {
            Blockbench.showQuickMessage(text('cnpc_immersiveboss.messages.fixed_arms', 'Fixed the legacy reversed victim arm layout.'));
        } else {
            Blockbench.showQuickMessage(existing
                ? text('cnpc_immersiveboss.messages.rig_completed_cameras', 'Completed the missing victim_root parts at the existing rig position.')
                : text('cnpc_immersiveboss.messages.rig_created', 'Created the victim_root player rig with three camera views.'));
        }
    }

    function refreshOverlays() {
        clearOverlays();
        // Canvas rebuilds can replace a cube mesh. Restore the original
        // materials first, then reapply the viewport-only skin to the current
        // victim meshes below.
        clearPlayerSkinOverrides();

        const showPreview = settingValue(SETTING_ENABLED, true);
        const hideAll = settingValue(SETTING_HIDE_ALL, false);
        if (typeof Project === 'undefined' || !Project) return;
        if (typeof Group === 'undefined' || !Array.isArray(Group.all)) return;
        if (typeof Cube === 'undefined' || !Array.isArray(Cube.all)) return;

        syncHiddenHitboxVisibility(Group.all, Cube.all, hideAll);
        if (showPreview) {
            Cube.all.forEach(cube => {
                if (!cube.parent || typeof cube.parent.name !== 'string') return;
                const type = classifyBone(cube.parent.name);
                if (!type) return;

                const overlay = createOverlay(cube, type, showPreview, hideAll);
                if (overlay) overlays.set(cube.uuid, overlay);
            });
        }
        applyPlayerSkinOverrides();
        loadDefaultPlayerSkinIfAvailable();
    }

    function scheduleRefresh() {
        if (refreshTimer !== null) clearTimeout(refreshTimer);
        refreshTimer = setTimeout(() => {
            refreshTimer = null;
            refreshOverlays();
        }, 0);
    }

    function runPreviewCleanup(stage, cleanup) {
        try {
            cleanup();
        } catch (error) {
            if (typeof console !== 'undefined' && typeof console.warn === 'function') {
                console.warn('[CNPC ImmersiveBoss] Failed to clean up ' + stage, error);
            }
        }
    }

    function onUnselectProject() {
        skinRequestSerial++;
        if (refreshTimer !== null) {
            clearTimeout(refreshTimer);
            refreshTimer = null;
        }
        runPreviewCleanup('hitbox overlays', clearOverlays);
        runPreviewCleanup('player skin', disposePlayerSkin);
        runPreviewCleanup('player skin root', removePlayerSkinRoot);
        if (resetCameraPreviewModel) {
            runPreviewCleanup('camera preview model', resetCameraPreviewModel);
        }
        defaultSkinAttempted = false;
        runPreviewCleanup('hitbox visibility', () => restoreHiddenHitboxVisibility(false));
    }

    function onCloseProject() {
        // Blockbench versions may emit close_project without first emitting
        // unselect_project. Invalidate requests and detach viewport objects
        // before the project's element tree is destroyed.
        onUnselectProject();
    }

    function onConvertFormat() {
        // Conversion has rebuilt Cube meshes. Ensure no stale temporary
        // objects remain and let the following update_view recreate them.
        skinRequestSerial++;
        runPreviewCleanup('hitbox overlays', clearOverlays);
        runPreviewCleanup('player skin overrides', clearPlayerSkinOverrides);
        runPreviewCleanup('player skin root', () => {
            if (playerSkinRoot && playerSkinRoot.parent) playerSkinRoot.parent.remove(playerSkinRoot);
        });
        scheduleRefresh();
    }

    function onSelectFormat() {
        // Format.convertTo() selects the destination format before it mutates
        // Outliner/Cube data. Tear down every viewport-only THREE child at
        // this point so conversion and tab switching never traverse plugin
        // objects left on the old model meshes.
        skinRequestSerial++;
        runPreviewCleanup('hitbox overlays', clearOverlays);
        runPreviewCleanup('player skin overrides', clearPlayerSkinOverrides);
        runPreviewCleanup('player skin root', () => {
            if (playerSkinRoot && playerSkinRoot.parent) playerSkinRoot.parent.remove(playerSkinRoot);
        });
    }

    function registerSetting(id, options) {
        options.plugin = PLUGIN_ID;
        const item = new Setting(id, options);
        settingsItems.push(item);
        return item;
    }

    function registerToggle(id, options) {
        const item = new Toggle(id, options);
        menuItems.push(item);
        MenuBar.addAction(item, 'view');
        return item;
    }

    Plugin.register(PLUGIN_ID, {
        title: text('cnpc_immersiveboss.plugin.title', 'CNPC ImmersiveBoss Hitbox Preview'),
        author: 'Sweda',
        description: text('cnpc_immersiveboss.plugin.description', 'Preview CNPC ImmersiveBoss hitbox bones as colored in-game wireframes.'),
        about: text('cnpc_immersiveboss.plugin.about', 'Shows every cube directly inside a valid h[a][d][b|s]_name bone as a colored OBB wireframe.\n\nUse Hide All IB hitboxes to hide hitbox groups and cubes. Create IB Player Rig adds the victim_root player puppet with hand-item attachment groups. Set Official Minecraft Player Skin previews a Mojang Java skin.\n\nAll previews are viewport-only and never change exported model geometry.'),
        icon: 'select_all',
        tags: ['Minecraft: Java Edition', 'GeckoLib', 'Utility'],
        version: '1.2.7',
        min_version: '4.10.0',
        variant: 'both',

        onload() {
            registerTranslations();
            materials = createMaterials();
            registerCameraPreviewPanel();

            registerSetting(SETTING_ENABLED, {
                name: text('cnpc_immersiveboss.settings.preview.name', 'ImmersiveBoss Hitbox Preview'),
                description: text('cnpc_immersiveboss.settings.preview.description', 'Show runtime-style wireframes for ImmersiveBoss hitbox bones.'),
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_AXES, {
                name: text('cnpc_immersiveboss.settings.axes.name', 'ImmersiveBoss Hitbox Axes'),
                description: text('cnpc_immersiveboss.settings.axes.description', 'Show local red, green, and blue axes inside each hitbox.'),
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_RUNTIME_VISIBILITY, {
                name: text('cnpc_immersiveboss.settings.runtime.name', 'ImmersiveBoss Runtime Visibility'),
                description: text('cnpc_immersiveboss.settings.runtime.description', 'Hide cube faces for hitbox bones without the appearance flag.'),
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_HIDE_ALL, {
                name: text('cnpc_immersiveboss.settings.hide_all.name', 'Hide All IB hitboxes'),
                description: text('cnpc_immersiveboss.settings.hide_all.description', 'Make all ImmersiveBoss hitbox groups and cubes invisible and unpickable in the viewport.'),
                category: 'view',
                value: false,
                onChange: scheduleRefresh
            });

            const condition = () => typeof Project !== 'undefined' && !!Project;
            registerToggle(PLUGIN_ID + '_hide_all_toggle', {
                name: text('cnpc_immersiveboss.actions.hide_all.name', 'Hide All IB hitboxes'),
                description: text('cnpc_immersiveboss.actions.hide_all.description', 'Hide ImmersiveBoss hitbox groups and cubes from rendering and viewport selection.'),
                icon: 'visibility_off',
                condition,
                linked_setting: SETTING_HIDE_ALL
            });

            playerSkinDialog = new Dialog({
                id: PLAYER_SKIN_ACTION + '_dialog',
                title: text('cnpc_immersiveboss.dialog.skin.title', 'Set Official Minecraft Player Skin'),
                lines: [
                    text('cnpc_immersiveboss.dialog.skin.line1', 'Enter a Minecraft Java username. The plugin fetches the current skin from Mojang.'),
                    text('cnpc_immersiveboss.dialog.skin.line2', 'The skin is used only for the Blockbench preview and is never exported.')
                ],
                form: {
                    username: {
                        label: text('cnpc_immersiveboss.dialog.skin.username', 'Minecraft Java username'),
                        type: 'text',
                        value: DEFAULT_PLAYER_SKIN,
                        placeholder: DEFAULT_PLAYER_SKIN
                    }
                },
                onConfirm(data) {
                    const username = String(data.username || '').trim();
                    if (!username) {
                        Blockbench.showMessageBox({
                            title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                            message: text('cnpc_immersiveboss.messages.enter_username', 'Enter a Minecraft Java username.')
                        });
                        return;
                    }
                    loadOfficialPlayerSkin(username);
                }
            });

            registerSetting(PLAYER_SKIN_ACTION, {
                type: 'click',
                name: text('cnpc_immersiveboss.actions.player_skin.name', 'Set Official Minecraft Player Skin'),
                description: text('cnpc_immersiveboss.actions.player_skin.description', 'Fetch a Java Edition player skin from Mojang and apply it to victim_root.'),
                category: 'view',
                icon: 'face',
                condition,
                click() {
                    if (getVictimCubes().length === 0) {
                        Blockbench.showMessageBox({
                            title: text('cnpc_immersiveboss.messages.title', 'CNPC ImmersiveBoss'),
                            message: text('cnpc_immersiveboss.messages.no_victim', 'This project has no victim_root player model. Run Create IB Player Rig first.')
                        });
                        return;
                    }
                    playerSkinDialog.show();
                }
            });
            const playerRigAction = new Action(PLAYER_RIG_ACTION, {
                name: text('cnpc_immersiveboss.actions.player_rig.name', 'Create IB Player Rig'),
                description: text('cnpc_immersiveboss.actions.player_rig.description', 'Create victim_root with vanilla skin layers, hand-item attachment groups, and throw cameras.'),
                icon: 'person_add',
                condition,
                click: createPlayerRig
            });
            menuItems.push(playerRigAction);
            MenuBar.addAction(playerRigAction, 'tools');

            REFRESH_EVENTS.forEach(event => Blockbench.on(event, scheduleRefresh));
            Blockbench.on('unselect_project', onUnselectProject);
            Blockbench.on('close_project', onCloseProject);
            Blockbench.on('select_format', onSelectFormat);
            Blockbench.on('convert_format', onConvertFormat);
            Blockbench.on('render_frame', syncPlayerSkinTransforms);
            scheduleRefresh();
        },

        onunload() {
            skinRequestSerial++;
            disposeCameraPreviewPanel();
            if (playerSkinDialog) {
                playerSkinDialog.delete();
                playerSkinDialog = null;
            }
            disposePlayerSkin();

            if (refreshTimer !== null) clearTimeout(refreshTimer);
            refreshTimer = null;

            REFRESH_EVENTS.forEach(event => Blockbench.removeListener(event, scheduleRefresh));
            Blockbench.removeListener('unselect_project', onUnselectProject);
            Blockbench.removeListener('close_project', onCloseProject);
            Blockbench.removeListener('select_format', onSelectFormat);
            Blockbench.removeListener('convert_format', onConvertFormat);
            Blockbench.removeListener('render_frame', syncPlayerSkinTransforms);
            clearOverlays();
            removePlayerSkinRoot();
            restoreHiddenHitboxVisibility();

            menuItems.reverse().forEach(item => item.delete());
            menuItems = [];
            settingsItems.reverse().forEach(item => item.delete());
            settingsItems = [];

            if (materials) {
                materials.wireframes.forEach(material => material.dispose());
                materials.axes.dispose();
                materials.hiddenFaces.dispose();
                materials = null;
            }
        }
    });
})();
