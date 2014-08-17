package com.github.jikoo.enchantedfurnace;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

/**
 * Class for tracking custom furnace properties and applying certain effects.
 * 
 * @author Jikoo
 */
public class Furnace {

	private Block b;
	private int cookModifier;
	private int burnModifier;
	private int fortune;
	private boolean canPause;
	private short frozenTicks = 0;

	public Furnace(Block b, int cookModifier, int burnModifier, int fortune, boolean canPause) {
		this(b, cookModifier, burnModifier, fortune, (short) (canPause ? 0 : -1));
	}

	public Furnace(Block b, int cookModifier, int burnModifier, int fortune, short frozenTicks) {
		this.b = b;
		this.cookModifier = cookModifier;
		this.burnModifier = burnModifier;
		this.fortune = fortune;
		this.canPause = frozenTicks >= 0;
		this.frozenTicks = frozenTicks;
	}

	public Block getBlock() {
		return b;
	}

	public org.bukkit.block.Furnace getFurnaceTile() {
		if (!b.getWorld().isChunkLoaded(b.getChunk())) {
			// Chunk must be loaded to get BlockState
			return null;
		}
		if (b.getType() == Material.FURNACE || b.getType() == Material.BURNING_FURNACE) {
			return (org.bukkit.block.Furnace) b.getState();
		}
		return null;
	}

	public int getCookModifier() {
		return this.cookModifier;
	}

	public int getBurnModifier() {
		return this.burnModifier;
	}

	public int getFortune() {
		return this.fortune;
	}

	public boolean canPause() {
		return this.canPause;
	}

	public void pause() {
		if (!this.canPause) {
			return;
		}

		// Delay to fix silk touch sometimes spitting out all items instead of smelting
		Bukkit.getScheduler().scheduleSyncDelayedTask(EnchantedFurnace.getInstance(), new Runnable() {

			@Override
			public void run() {
				org.bukkit.block.Furnace f = getFurnaceTile();
				frozenTicks = f.getBurnTime();
				f.setBurnTime((short) 0);
				f.update(true);
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void resume() {
		org.bukkit.block.Furnace f = this.getFurnaceTile();
		// Is furnace unfrozen already?
		if (f.getBurnTime() > 0 || this.frozenTicks < 1) {
			return;
		}

		// Is there an input?
		FurnaceInventory i = f.getInventory();
		if (i.getSmelting() == null) {
			return;
		}

		boolean viableMatch = false;
		if (i.getResult() != null) {
			// Verify existing result can be obtained from input before restarting
			Iterator<Recipe> ri = Bukkit.getRecipesFor(i.getResult()).iterator();
			while (ri.hasNext()) {
				Recipe r = ri.next();
				if (r instanceof FurnaceRecipe) {
					if (((FurnaceRecipe) r).getInput().getType() == f.getInventory().getSmelting().getType()) {
						if (r.getResult().getType() != i.getResult().getType()) {
							return;
						}
						break;
					}
				}
			}
		} else {
			// If there is no result, verify that the smelting item can produce a result
			Iterator<Recipe> ri = Bukkit.recipeIterator();
			while (ri.hasNext()) {
				Recipe r = ri.next();
				if (!(r instanceof FurnaceRecipe)) {
					continue;
				}
				ItemStack input = ((FurnaceRecipe) r).getInput();
				if (input.getType() != i.getSmelting().getType()) {
					continue;
				}
				if (input.getData().getData() > -1 && !input.getData().equals(i.getSmelting().getData())) {
					continue;
				}
				// Unlike fortune, we don't need to find the exact match so long as there is a working match.
				viableMatch = true;
				break;
			}
		}
		if (!viableMatch) {
			return;
		}
	}

	public boolean isPaused() {
		return this.frozenTicks > 0;
	}

	public short getFrozenTicks() {
		return this.frozenTicks;
	}
}
