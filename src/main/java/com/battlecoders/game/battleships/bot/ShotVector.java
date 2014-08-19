package com.battlecoders.game.battleships.bot;

import com.battlecoders.game.battleships.bot.entity.Shot;

public class ShotVector {
	
	public enum VectorType{X, Y};
	
	private VectorType vectorType;
	private Shot startShot;
	private Shot lastShot;
	private int currentDirection;
	public ShotVector(VectorType vectorType, Shot startShot, Shot currentShot,
			int currentDirection) {
		super();
		this.vectorType = vectorType;
		this.startShot = startShot;
		this.lastShot = currentShot;
		this.currentDirection = currentDirection;
	}
	public VectorType getVectorType() {
		return vectorType;
	}
	public Shot getStartShot() {
		return startShot;
	}
	public Shot getLastShot() {
		return lastShot;
	}
	public int getCurrentDirection() {
		return currentDirection;
	}
}
