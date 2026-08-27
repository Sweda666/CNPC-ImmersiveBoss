(function() {
    'use strict';

    const PLUGIN_ID = 'cnpc_immersiveboss_hitbox_preview';
    const SETTING_ENABLED = PLUGIN_ID + '_enabled';
    const SETTING_AXES = PLUGIN_ID + '_axes';
    const SETTING_RUNTIME_VISIBILITY = PLUGIN_ID + '_runtime_visibility';
    const SETTING_HIDE_ALL = PLUGIN_ID + '_hide_all';

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
    const overlays = new Map();
    const hiddenGroupVisibility = new Map();
    const hiddenCubeVisibility = new Map();

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

    function restoreHiddenHitboxVisibility() {
        const groupsChanged = restoreTrackedVisibility(hiddenGroupVisibility);
        const cubesChanged = restoreTrackedVisibility(hiddenCubeVisibility);
        if (groupsChanged || cubesChanged) updateCanvasVisibility();
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

    function refreshOverlays() {
        clearOverlays();

        const showPreview = settingValue(SETTING_ENABLED, true);
        const hideAll = settingValue(SETTING_HIDE_ALL, false);
        if (typeof Project === 'undefined' || !Project) return;
        if (typeof Group === 'undefined' || !Array.isArray(Group.all)) return;
        if (typeof Cube === 'undefined' || !Array.isArray(Cube.all)) return;

        syncHiddenHitboxVisibility(Group.all, Cube.all, hideAll);
        if (!showPreview) return;

        Cube.all.forEach(cube => {
            if (!cube.parent || typeof cube.parent.name !== 'string') return;
            const type = classifyBone(cube.parent.name);
            if (!type) return;

            const overlay = createOverlay(cube, type, showPreview, hideAll);
            if (overlay) overlays.set(cube.uuid, overlay);
        });
    }

    function scheduleRefresh() {
        if (refreshTimer !== null) clearTimeout(refreshTimer);
        refreshTimer = setTimeout(() => {
            refreshTimer = null;
            refreshOverlays();
        }, 0);
    }

    function onUnselectProject() {
        if (refreshTimer !== null) {
            clearTimeout(refreshTimer);
            refreshTimer = null;
        }
        clearOverlays();
        restoreHiddenHitboxVisibility();
    }

    function registerSetting(id, options) {
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
        title: 'CNPC ImmersiveBoss Hitbox Preview',
        author: 'Sweda',
        description: 'Previews CNPC ImmersiveBoss hitbox bones as in-game colored wireframes.',
        about: [
            'Shows every cube directly inside a valid `h[a][d][b|s]_name` bone as a colored OBB wireframe.',
            '',
            '- White: physical and detectable',
            '- Blue: physical',
            '- Yellow: detectable sensor',
            '- Green: sensor',
            '',
            'Use `Hide All ImmersiveBoss Hitboxes` to make every hitbox bone group and cube invisible and unpickable in the viewport.',
            '',
            'The hide-all option restores each group\'s and cube\'s previous visibility when disabled or when the plugin unloads.',
            '',
            'The preview follows Blockbench bone animation and never changes exported model geometry.'
        ].join('\n'),
        icon: 'select_all',
        tags: ['Minecraft: Java Edition', 'GeckoLib', 'Utility'],
        version: '1.1.5',
        min_version: '4.10.0',
        variant: 'both',

        onload() {
            materials = createMaterials();

            registerSetting(SETTING_ENABLED, {
                name: 'ImmersiveBoss Hitbox Preview',
                description: 'Show runtime-style wireframes for ImmersiveBoss hitbox bones.',
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_AXES, {
                name: 'ImmersiveBoss Hitbox Axes',
                description: 'Show the local red, green, and blue axes inside each hitbox.',
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_RUNTIME_VISIBILITY, {
                name: 'ImmersiveBoss Runtime Visibility',
                description: 'Hide cube faces for hitbox bones without the appearance flag.',
                category: 'view',
                value: true,
                onChange: scheduleRefresh
            });
            registerSetting(SETTING_HIDE_ALL, {
                name: 'Hide All ImmersiveBoss Hitboxes',
                description: 'Make all hitbox bone groups and cubes invisible and unpickable in the viewport.',
                category: 'view',
                value: false,
                onChange: scheduleRefresh
            });

            const condition = () => typeof Project !== 'undefined' && !!Project;
            registerToggle(PLUGIN_ID + '_toggle', {
                name: 'ImmersiveBoss Hitbox Preview',
                description: 'Toggle colored ImmersiveBoss hitbox wireframes.',
                icon: 'select_all',
                condition,
                linked_setting: SETTING_ENABLED
            });
            registerToggle(PLUGIN_ID + '_hide_all_toggle', {
                name: 'Hide All ImmersiveBoss Hitboxes',
                description: 'Hide hitbox bone groups and cubes from rendering and viewport selection.',
                icon: 'visibility_off',
                condition,
                linked_setting: SETTING_HIDE_ALL
            });
            registerToggle(PLUGIN_ID + '_axes_toggle', {
                name: 'ImmersiveBoss Hitbox Axes',
                description: 'Toggle local XYZ axes for ImmersiveBoss hitboxes.',
                icon: 'open_with',
                condition,
                linked_setting: SETTING_AXES
            });
            registerToggle(PLUGIN_ID + '_runtime_visibility_toggle', {
                name: 'ImmersiveBoss Runtime Visibility',
                description: 'Hide faces of hitbox bones that do not use the appearance flag.',
                icon: 'visibility_off',
                condition,
                linked_setting: SETTING_RUNTIME_VISIBILITY
            });

            REFRESH_EVENTS.forEach(event => Blockbench.on(event, scheduleRefresh));
            Blockbench.on('unselect_project', onUnselectProject);
            scheduleRefresh();
        },

        onunload() {
            if (refreshTimer !== null) clearTimeout(refreshTimer);
            refreshTimer = null;

            REFRESH_EVENTS.forEach(event => Blockbench.removeListener(event, scheduleRefresh));
            Blockbench.removeListener('unselect_project', onUnselectProject);
            clearOverlays();
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
