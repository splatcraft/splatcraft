package com.cibernet.splatcraft.items;

import com.cibernet.splatcraft.blocks.IInked;
import com.cibernet.splatcraft.registries.SplatCraftBlocks;
import com.cibernet.splatcraft.tileentities.TileEntityColor;
import com.cibernet.splatcraft.utils.ColorItemUtils;
import com.cibernet.splatcraft.utils.InkColors;
import com.cibernet.splatcraft.utils.SplatCraftUtils;
import com.cibernet.splatcraft.utils.TabSplatCraft;
import com.cibernet.splatcraft.world.save.SplatCraftPlayerData;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import java.util.List;

import static com.cibernet.splatcraft.utils.ColorItemUtils.*;

public class ItemColorChanger extends ItemCoordSet
{
	public ItemColorChanger()
	{
		setUnlocalizedName("colorChanger");
		setRegistryName("color_changer");
		setMaxStackSize(1);
		setCreativeTab(TabSplatCraft.main);
		ColorItemUtils.inkColorItems.add(this);
		
		this.addPropertyOverride(new ResourceLocation("active"), new IItemPropertyGetter()
		{
			@SideOnly(Side.CLIENT)
			public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
			{
				return hasCoordSet(stack) ? 1.0F : 0.0F;
			}
		});
	}
	
	@Override
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
	{
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if(isColorLocked(stack))
		{
			int color = getInkColor(stack);
			tooltip.add(SplatCraftUtils.getColorName(color));
		}
	}
	
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected)
	{
		if(isColorLocked(stack) || !(entityIn instanceof EntityPlayer))
			return;
		
		
		setInkColor(stack, SplatCraftPlayerData.getInkColor((EntityPlayer) entityIn));
		
		super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
	}
	
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem)
	{
		BlockPos pos = new BlockPos(entityItem.posX, entityItem.posY-1, entityItem.posZ);
		
		ItemStack stack = entityItem.getItem();
		if(entityItem.world.getBlockState(pos).getBlock().equals(SplatCraftBlocks.inkwell))
		{
			if(entityItem.world.getTileEntity(pos) instanceof TileEntityColor)
			{
				TileEntityColor te = (TileEntityColor) entityItem.world.getTileEntity(pos);
				
				if(getInkColor(stack) != te.getColor() || !isColorLocked(stack))
				{
					setInkColor(stack, te.getColor());
					setColorLocked(stack, true);
				}
			}
		}
		else if (entityItem.world.getBlockState(pos.up()).getMaterial().equals(Material.WATER) && isColorLocked(stack))
		{
			setInkColor(stack, InkColors.DYE_WHITE.getColor());
			setColorLocked(stack, false);
		}
		
		return super.onEntityItemUpdate(entityItem);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer playerIn, EnumHand handIn)
	{
		ItemStack stack = playerIn.getHeldItem(handIn);
		if(!hasCoordSet(stack))
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		
		BlockPos[] coordSet = getCoordSet(stack);
		BlockPos blockpos = coordSet[0];
		BlockPos blockpos1 = coordSet[1];
		BlockPos blockpos2 = new BlockPos(Math.min(blockpos.getX(), blockpos1.getX()), Math.min(blockpos.getY(), Math.min(blockpos1.getY(), blockpos.getY())), Math.min(blockpos.getZ(), blockpos1.getZ()));
		BlockPos blockpos3 = new BlockPos(Math.max(blockpos.getX(), blockpos1.getX()), Math.max(blockpos.getY(), Math.max(blockpos1.getY(), blockpos.getY())), Math.max(blockpos.getZ(), blockpos1.getZ()));
		
		if (!(blockpos2.getY() >= 0 && blockpos3.getY() < 256))
			playerIn.sendStatusMessage(new TextComponentTranslation("commands.clearInk.outOfWorld"), true);
		
		
		for(int j = blockpos2.getZ(); j <= blockpos3.getZ(); j += 16)
		{
			for(int k = blockpos2.getX(); k <= blockpos3.getX(); k += 16)
			{
				if(!world.isBlockLoaded(new BlockPos(k, blockpos3.getY() - blockpos2.getY(), j)))
				{
					playerIn.sendStatusMessage(new TextComponentTranslation("commands.clearInk.outOfWorld"), true);
				}
			}
		}
		int count = 0;
		int color = ColorItemUtils.getInkColor(stack);
		for(int x = blockpos2.getX(); x <= blockpos3.getX(); x++)
			for(int y = blockpos2.getY(); y <= blockpos3.getY(); y++)
				for(int z = blockpos2.getZ(); z <= blockpos3.getZ(); z++)
				{
					BlockPos pos = new BlockPos(x,y,z);
					IBlockState state = world.getBlockState(pos);
					Block block = state.getBlock();
					if(block instanceof IInked)
					{
						((TileEntityColor)world.getTileEntity(pos)).setColor(color);
						world.notifyBlockUpdate(pos, state, state, 3);
						count++;
					}
				}
		playerIn.sendStatusMessage(new TextComponentString(I18n.format("commands.changeColor.success", count, SplatCraftUtils.getColorName(color))), true);
		
		return new ActionResult<>(EnumActionResult.SUCCESS, stack);
	}
}
