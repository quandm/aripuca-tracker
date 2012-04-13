package com.aripuca.tracker.util;

import java.util.ArrayList;
import java.util.List;

public abstract class ContainerCarousel {

	protected int current = 0;

	protected List<Integer> containers;
	
	protected int resourceId;

	public ContainerCarousel() {

		containers = new ArrayList<Integer>();

		initialize();
		
	}

	protected abstract void initialize();
	
	public int getResourceId() {
		
		return resourceId;
		
	}

	public int getCurrentContainer() {
		return containers.get(current);
	}

	public int getNextContainer() {
		if (current < containers.size() - 1) {
			current++;
		} else {
			current = 0;
		}
		return containers.get(current);
	}

	public int getCurrentContainerId() {

		return current;

	}

	public void setCurrentContainerId(int newCurrent) {
		if (newCurrent > containers.size() - 1) {
			current = 0;
		} else {
			current = newCurrent;
		}
	}

}
