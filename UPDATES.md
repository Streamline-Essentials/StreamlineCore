+ Added game client support for Fabric, Forge and NeoForge (1.20.1, 1.21.1, 1.21.11, 26.2, 26.3), plus a client-only Forge 1.8.9 build.
+ Added client modules, which install, update and unload while the game runs, from eCloud or the client/modules folder.
+ Added world modules: server modules that a client runs in the singleplayer and LAN worlds it hosts (not on Forge 1.8.9).
+ Added the StreamlineClient menu (Right Shift opens the HUD editor), with Client Settings, module pages, settings, key bindings and an eCloud browser.
+ Added the HUD editor: drag, scale, nudge with the arrow keys, hide and delete elements, and edit their text with placeholders.
+ Added the /streamlineclient (/slclient) command to manage client modules and run Streamline commands on a hosted world.
+ Added the Client Settings page (Show HUD, Remember Last Page, Check eCloud For Updates, chroma colors), which was missing from the 2.6.1.2 jars.
+ Added chroma color placeholders (%streamlineclient_colors_chroma%).
+ Fixed left clicks on 26.3 counting as right clicks in the StreamlineClient menu and HUD editor, which stopped dragging elements, the HUD editor's menu button and key binding.
+ Text fields in the StreamlineClient menu can select text (drag, Shift with arrows, Ctrl+A), copy, cut and delete whole words.
+ Client modules can add rebindable keys on their menu page and hold sprint or sneak, which ClientTogglesModule needs to load.
+ Added the StreamlineHUD (FPS, ping, clocks and more) and ClientTogglesModule (toggle sprint and toggle sneak) client modules.
+ Modules now declare the platforms they run on, and Streamline only loads modules made for its platform.
+ Fixed a lot of bugs with Database handling.
+ Added CosmicGuis and handling.
+ Changed to using https://modules.drak.gg
