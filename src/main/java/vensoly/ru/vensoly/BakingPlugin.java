package vensoly.ru.vensoly; // Замените на ваш пакет

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Furnace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import static org.bukkit.ChatColor.*;

public class BakingPlugin extends JavaPlugin implements Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Baking Plugin enabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block == null) return;

            Block furnaceBlock = block.getRelative(BlockFace.UP);
            if (furnaceBlock.getType() != Material.FURNACE) return;

            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType() == Material.AIR) return;

            try {
                if (isBakingIngredient(itemInHand)) {
                    BlockState state = furnaceBlock.getState();
                    if (!(state instanceof org.bukkit.block.Furnace)) {
                        player.sendMessage(RED + "Это не печь!");
                        return;
                    }
                    Furnace furnace = (Furnace) state.getBlockData();
                    FurnaceInventory furnaceInventory = ((org.bukkit.block.Furnace) state).getInventory();
                    ItemStack fuel = furnaceInventory.getFuel();

                    if (fuel != null && fuel.getType() != Material.AIR) {
                        bakeItem(player, itemInHand);
                    } else {
                        player.sendMessage(DARK_RED + "Печь не горит или нет топлива!");
                    }
                }
            } catch (ClassCastException | NullPointerException e) {
                getLogger().warning("Ошибка при выпечке: " + e.getMessage());
            }
        }
    }

    private boolean isBakingIngredient(ItemStack item) {
        if (config == null) {
            getLogger().severe("Конфигурационный файл не загружен!");
            return false;
        }
        return config.getStringList("ingredients").contains(item.getType().name());
    }

    private void bakeItem(Player player, ItemStack ingredient) {
        if (config == null) {
            getLogger().severe("Конфигурационный файл не загружен!");
            return;
        }
        String itemName = ingredient.getType().name();
        String bakedItemName = config.getString("recipes." + itemName + ".result");
        int bakedItemCount = config.getInt("recipes." + itemName + ".count", 1);

        if (bakedItemName != null) {
            Material bakedMaterial = Material.matchMaterial(bakedItemName);
            if (bakedMaterial != null) {
                ItemStack bakedItem = new ItemStack(bakedMaterial, bakedItemCount);
                ItemMeta meta = bakedItem.getItemMeta();
                String displayName = translateAlternateColorCodes('&', config.getString("recipes." + itemName + ".displayName"));
                if (displayName != null) meta.setDisplayName(displayName);
                bakedItem.setItemMeta(meta);
                if (!player.getInventory().addItem(bakedItem).isEmpty()) {
                    player.sendMessage(RED + "Недостаточно места в инвентаре!");
                } else {
                    player.sendMessage(GREEN + "Выпечка готова!");
                }
                return;
            }
        }
        player.sendMessage(RED + "Ошибка выпечки!");
    }
}
