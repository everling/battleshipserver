package serv;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.sun.net.httpserver.*;


public class Server {
	
	private volatile String msg = "Hello World";
	private volatile List<Ship> shipsP1;
	private volatile List<Ship> shipsP2;
	private volatile List<String> shotAtP1 = new ArrayList<String>();
	private volatile List<String> shotAtP2 = new ArrayList<String>();
	private volatile List<String> commandHistory = new ArrayList<String>();
	private final int grid = 5;

	
	public void run() throws Exception{
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
	    server.createContext("/game", new MultiplayerServer());
	    server.setExecutor(null); // creates a default executor
	    server.start();
	    
	    Scanner sc = new Scanner(System.in);
        
	    int t = 0;
	    
        while(sc.hasNextLine()){
        	msg = sc.nextLine();
        	
        	//command
        	String[] ss = msg.split(" ");
        	if(ss.length == 2 && ss[0].equals("init")){
        		System.out.println(initPlayer(ss[1]));
        	}
        	if(ss.length == 2 && ss[0].equals("draw")){
        		draw(ss[1]);
        	}
        	if(ss.length > 1 && ss[0].equals("cmd")){
        		
        		String command = ss[1];
        		if(ss.length == 3)
        			command += " "+ss[2];
        		
        		String result = playerCommand(command);
        		
        		String broadcast = t++ + " " +command +" " +result;
        		msg = broadcast;
        		System.out.println(broadcast);
        		commandHistory.add(broadcast);
        		
        		if(result.equals("X")){
        			String winner = returnWin();
        			if(winner != null){
        				broadcast = t++ +" " +winner +" " +"WIN";
        				msg = broadcast;
                		System.out.println(broadcast);
                		commandHistory.add(broadcast);
        			}
        		}
        		
        		
        	}
        	
        }
		
	}
	
	
	private String returnWin(){
		
		if(shipsP1 == null || shipsP2 == null)
			return null;
		
		boolean win = true;
		for(Ship s : shipsP2){
			if(!s.isSunk()){
				win = false;
				break;
			}
		}
		if(win)
			return "P1"; 
		
		win = true;
		for(Ship s : shipsP1){
			if(!s.isSunk()){
				win = false;
				break;
			}
		}
		if(win)
			return "P2"; 
		
		return null;
	}
	
	
	
	/**
	 * Client tells server of ship setup
	 * @param positions the set of ship positions on a format like "A1A2xB3C3D3xE1E2", x delimits ships
	 * @return player identity P1 or P2 if OK
	 */
	public String initPlayer(String positions){
		
		String[] shipCoors = positions.split("x");
		
		List<Ship> ships = new ArrayList<Ship>();
		
		for(String s : shipCoors){
			if(s.length() % 2 > 0)
				return "Bad ship coordinate";
			Ship ship = new Ship(s);
			ships.add(ship);
			
		}
		
		if(shipsP1 == null){
			shipsP1 = ships;
			return "P1";
		}
		else if(shipsP2 == null){
			shipsP2 = ships;
			return "P2";
		}
	
		return "No player slot";
	}
	
	
	private void draw(String player){
		
		List<String> shotAt = null;
		List<Ship> ships = null;
		
		if(player.equals("P1")){
			shotAt = shotAtP1;
			ships = shipsP1;
			
		}else if(player.equals("P2")){
			shotAt = shotAtP2;
			ships = shipsP2;
		}
		else{
			return;
		}
		
		String alpha = "ABCDEFGHIJKLMOP";
		
		System.out.print("\t");
		for(int i = 0; i < grid; i++)
			System.out.print((i+1)+"\t");
		System.out.println();
		
		for(int i = 0; i < grid; i++){
			String ch = alpha.substring(i,i+1);
			System.out.print(ch +"\t");

			for(int j = 0; j < grid; j++){
				String pos = ch + (j+1);
				
				String toDraw = null;
				
				for(Ship s : ships){
					if(s.occupies(pos) && shotAt.contains(pos))
						toDraw = "x";
					else if(s.occupies(pos))
						toDraw = ""+ships.indexOf(s);
				}
				
				if(toDraw == null && shotAt.contains(pos))
					toDraw = "o";
				else if (toDraw == null)
					toDraw = "";
				
				System.out.print(toDraw+"\t");

			}
			System.out.println();

			
		}
		
		
	}
	
	/**
	 * This is the interface between the speech server and the game server.
	 * Valid commands (and feedback for invalid commands) is broadcasted to the game clients.
	 * @param command Attack on the form "P1 A5" for P1 to strike A1 on P2. 
	 * @return 	"x" -> hit
	 * 			"X" -> hit and sunk
	 * 			"o" -> miss
	 * 			"." -> previously shot
	 * 			"S" -> stop previously issued command
	 */
	public String playerCommand(String command){
		
		String[] c = command.split(" ");
		
		if(c.length != 2)
			return "bad length";

		String attack = c[1];
		
		if(attack.equals("STOP"))
			return "S"; // stop previous issued command
		
		List<String> shotAt = null;
		List<Ship> ships = null;
		
		if(c[0].equals("P1")){
			shotAt = shotAtP2;
			ships = shipsP2;
			
		}else if(c[0].equals("P2")){
			shotAt = shotAtP1;
			ships = shipsP1;
		}
		else{
			return "bad player";
		}

		if(shotAt.contains(attack)){
			return "."; //previously shot
		}
		
		shotAt.add(attack);
		
		for(Ship s : ships){
			if(s.isHit(attack)){
				if(s.isSunk())
					return "X";
				return "x";
			}
		}
		return "o";
		
		
	}
	
    public static void main(String[] args){
    	
    	
    	try {    	
    		Server s = new Server();
			s.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

     
    class MultiplayerServer implements HttpHandler {
         
    	@Override
        public void handle(HttpExchange tt) throws IOException {
        	
        	String params = tt.getRequestURI().getQuery();
            String response = "";

        	int t = -1;
        	if(params != null){
        		for (String p : params.split("&")) {
                    String pair[] = p.split("=");
                    if (pair.length>1 && pair[0].equals("t")) {
                    	t = Integer.parseInt(pair[1]);
                    }
                    else if (pair.length>1 && pair[0].equals("init")) {
                    	response = initPlayer(pair[1]);
                    	
                    }
                }
        	}

        	if(t == -1 && response.length() == 0){
        		response = msg;
        	}
        	
        	if(t > -1 && t < commandHistory.size()){
        		response = commandHistory.get(t);
        	}
        	
            tt.sendResponseHeaders(200, response.length());
            OutputStream os = tt.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}