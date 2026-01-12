package com.github.ars_zero.common.glyph.augment;

import com.github.ars_zero.ArsZero;
import com.github.ars_zero.common.shape.BaseShape;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;

public class AugmentCube extends AbstractAugment implements IShapeAugment {
  public static final String ID = "augment_cube";
  public static final AugmentCube INSTANCE = new AugmentCube();

  private AugmentCube() {
    super(ArsZero.prefix(ID), "Cube");
  }

  @Override
  public BaseShape getShape() {
    return BaseShape.CUBE;
  }

  @Override
  public int getDefaultManaCost() {
    return 0;
  }

  @Override
  public SpellTier defaultTier() {
    return SpellTier.TWO;
  }

  @Override
  public String getBookDescription() {
    return "Used with Convergence to generate cube shapes. This is the default shape. When flattened, produces squares.";
  }
}
