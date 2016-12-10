package serv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ship {

	private List<String> occupies;
	private List<String> hit;
	
	
	
	/**
	 * 
	 * @param coordinates a concatenated list of grid positions e.g. "A3B3C3"
	 */
	public Ship(String coordinates){
		
		occupies = new ArrayList<String>();
		hit = new ArrayList<String>();
		
		for(int i = 0; i < coordinates.length() - 1; i += 2){
			String c = coordinates.substring(i, i+2);
			occupies.add(c);
		}
		
	}
	
	public boolean occupies(String coordinate){
		return occupies.contains(coordinate);
	}
	
	public boolean isHit(String coordinate){
		if(occupies(coordinate)){
			if(!hit.contains(coordinate))hit.add(coordinate);
			return true;
		}
		return false;
	}
	
	public boolean isSunk(){
		return occupies.size() == hit.size();
	}
	
}
