package me.libraryaddict.disguise.utilities.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.DisguiseConfig;
import me.libraryaddict.disguise.LibsDisguises;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.libraryaddict.disguise.utilities.DisguiseUtilities;
import me.libraryaddict.disguise.utilities.packets.packetlisteners.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class PacketsManager {
    private static PacketListener clientInteractEntityListener;
    private static PacketListener inventoryListener;
    private static boolean inventoryModifierEnabled;
    private static LibsDisguises libsDisguises;
    private static PacketListener mainListener;
    private static PacketListener soundsListener;
    private static boolean soundsListenerEnabled;
    private static PacketListener viewDisguisesListener;
    private static boolean viewDisguisesListenerEnabled;
    private static PacketsHandler packetsHandler;

    public static void addPacketListeners() {
        // Add a client listener to cancel them interacting with uninteractable disguised entitys.
        // You ain't supposed to be allowed to 'interact' with a item that cannot be clicked.
        // Because it kicks you for hacking.

        clientInteractEntityListener = new PacketListenerClientInteract(libsDisguises);
        PacketListener tabListListener = new PacketListenerTabList(libsDisguises);

        ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(clientInteractEntityListener)
                .syncStart();
        ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(tabListListener).syncStart();

        // Now I call this and the main listener is registered!
        setupMainPacketsListener();
    }

    /**
     * Creates the packet listeners
     */
    public static void init(LibsDisguises plugin) {
        libsDisguises = plugin;
        soundsListener = new PacketListenerSounds(libsDisguises);

        // Self disguise (/vsd) listener
        viewDisguisesListener = new PacketListenerViewSelfDisguise(libsDisguises);

        inventoryListener = new PacketListenerInventory(libsDisguises);
        packetsHandler = new PacketsHandler();
    }

    public static PacketsHandler getPacketsHandler() {
        return packetsHandler;
    }

    public static boolean isHearDisguisesEnabled() {
        return soundsListenerEnabled;
    }

    public static boolean isInventoryListenerEnabled() {
        return inventoryModifierEnabled;
    }

    public static boolean isViewDisguisesListenerEnabled() {
        return viewDisguisesListenerEnabled;
    }

    public static void setHearDisguisesListener(boolean enabled) {
        if (soundsListenerEnabled != enabled) {
            soundsListenerEnabled = enabled;

            if (soundsListenerEnabled) {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(soundsListener)
                        .syncStart();
            } else {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().unregisterAsyncHandler(soundsListener);
            }
        }
    }

    public static void setInventoryListenerEnabled(boolean enabled) {
        if (inventoryModifierEnabled != enabled) {
            inventoryModifierEnabled = enabled;

            if (inventoryModifierEnabled) {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(inventoryListener)
                        .syncStart();
            } else {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().unregisterAsyncHandler(inventoryListener);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                Disguise disguise = DisguiseAPI.getDisguise(player, player);

                if (disguise != null) {
                    if (viewDisguisesListenerEnabled && disguise.isSelfDisguiseVisible() &&
                            (disguise.isHidingArmorFromSelf() || disguise.isHidingHeldItemFromSelf())) {
                        player.updateInventory();
                    }
                }
            }
        }
    }

    public static void setupMainPacketsListener() {
        if (clientInteractEntityListener != null) {
            if (mainListener != null) {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().unregisterAsyncHandler(mainListener);
            }

            ArrayList<PacketType> packetsToListen = new ArrayList<>();
            // Add spawn packets
            {
                packetsToListen.add(Server.NAMED_ENTITY_SPAWN);
                packetsToListen.add(Server.SPAWN_ENTITY_EXPERIENCE_ORB);
                packetsToListen.add(Server.SPAWN_ENTITY);
                packetsToListen.add(Server.SPAWN_ENTITY_LIVING);
                packetsToListen.add(Server.SPAWN_ENTITY_PAINTING);
            }

            // Add packets that always need to be enabled to ensure safety
            {
                packetsToListen.add(Server.ENTITY_METADATA);
            }

            if (DisguiseConfig.isCollectPacketsEnabled()) {
                packetsToListen.add(Server.COLLECT);
            }

            if (DisguiseConfig.isMiscDisguisesForLivingEnabled()) {
                packetsToListen.add(Server.UPDATE_ATTRIBUTES);
            }

            // Add movement packets
            if (DisguiseConfig.isMovementPacketsEnabled()) {
                packetsToListen.add(Server.ENTITY_LOOK);
                packetsToListen.add(Server.REL_ENTITY_MOVE_LOOK);
                packetsToListen.add(Server.ENTITY_HEAD_ROTATION);
                packetsToListen.add(Server.ENTITY_TELEPORT);
                packetsToListen.add(Server.REL_ENTITY_MOVE);
                packetsToListen.add(Server.ENTITY_VELOCITY);
            }

            // Add equipment packet
            if (DisguiseConfig.isEquipmentPacketsEnabled()) {
                packetsToListen.add(Server.ENTITY_EQUIPMENT);
            }

            // Add the packet that ensures if they are sleeping or not
            if (DisguiseConfig.isAnimationPacketsEnabled()) {
                packetsToListen.add(Server.ANIMATION);
            }

            // Add the packet that makes sure that entities with armor do not send unpickupable armor on death
            if (DisguiseConfig.isEntityStatusPacketsEnabled()) {
                packetsToListen.add(Server.ENTITY_STATUS);
            }

            mainListener = new PacketListenerMain(libsDisguises, packetsToListen);

            ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(mainListener)
                    .syncStart();
        }
    }

    public static void setViewDisguisesListener(boolean enabled) {
        if (viewDisguisesListenerEnabled != enabled) {
            viewDisguisesListenerEnabled = enabled;

            if (viewDisguisesListenerEnabled) {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().registerAsyncHandler(mainListener)
                        .syncStart();
            } else {
                ProtocolLibrary.getProtocolManager().getAsynchronousManager().unregisterAsyncHandler(mainListener);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                Disguise disguise = DisguiseAPI.getDisguise(player, player);

                if (disguise != null) {
                    if (disguise.isSelfDisguiseVisible()) {
                        if (enabled) {
                            DisguiseUtilities.setupFakeDisguise(disguise);
                        } else {
                            DisguiseUtilities.removeSelfDisguise(player);
                        }

                        if (inventoryModifierEnabled &&
                                (disguise.isHidingArmorFromSelf() || disguise.isHidingHeldItemFromSelf())) {
                            player.updateInventory();
                        }
                    }
                }
            }
        }
    }
}
