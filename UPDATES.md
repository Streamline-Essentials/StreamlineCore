+ Added game client support for Fabric, Forge and NeoForge (1.20.1, 1.21.1, 1.21.11, 26.2, 26.3), plus a client-only Forge 1.8.9 build.
+ Added client modules, which install, update and unload while the game runs, from eCloud or the client/modules folder.
+ Added world modules: server modules that a client runs in the singleplayer and LAN worlds it hosts (not on Forge 1.8.9).
+ Added the StreamlineClient menu (Right Shift opens the HUD editor), with Client Settings, module pages, settings, key bindings and an eCloud browser.
+ Added the HUD editor: drag, scale, nudge with the arrow keys, hide and delete elements, and edit their text with placeholders.
+ Added the /streamlineclient (/slclient) command to manage client modules and run Streamline commands on a hosted world.
+ Added chroma color placeholders (%streamlineclient_colors_chroma%).
+ Added the StreamlineHUD (FPS, ping, clocks and more) and ClientTogglesModule (toggle sprint and toggle sneak) client modules.
+ Modules now declare the platforms they run on, and Streamline only loads modules made for its platform.
+ Fixed a lot of bugs with Database handling.
+ Added CosmicGuis and handling.
+ Changed to using https://modules.drak.gg
