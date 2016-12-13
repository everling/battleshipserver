package serv;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class SpeechClient {       
                                     
	
	private String command = "";
	private String alpha = "(ALPHA|BRAVO|CHARLIE|DELTA|ECHO)";
	
	private String serverUrl = "http://localhost:8000/game";
	private String player = "P1";
	
	
	private void disableLogging(){
        Logger cmRootLogger = Logger.getLogger("default.config");
        cmRootLogger.setLevel(java.util.logging.Level.OFF);
        String conFile = System.getProperty("java.util.logging.config.file");
        if (conFile == null) {
              System.setProperty("java.util.logging.config.file", "ignoreAllSphinx4LoggingOutput");
        }
	}
	
	public void run() throws IOException{
        Configuration configuration = new Configuration();
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("file:language model/9387.dic");
        configuration.setLanguageModelPath("file:language model/9387.lm");
        
        disableLogging();
        
        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
        recognizer.startRecognition(true);
        
        SpeechResult result = recognizer.getResult();
        
        while ((result = recognizer.getResult()) != null) {
           
        	String hypothesis = result.getHypothesis();
        	System.out.format("\nUtterance hypothesis: %s\n", hypothesis);
            
        	if(hypothesis.length() > 0){
            	for(String token : hypothesis.split(" ")){
            		feedToken(token.trim());
            	}
            	String cmd = getCommand();
            	if(cmd != null){
            		sendCmd(cmd);
            		
            	}
        	}

        	
        }
        recognizer.stopRecognition();
        
		
	}
	
	/**
	 * Sents GET request to server with a command
	 * @param cmd
	 * @throws IOException
	 */
	private void sendCmd(String cmd){
		
		System.out.println("Sending command " +cmd);

		
		String url = serverUrl +"?v=" +makeSendable(cmd);
		URL obj;
		try {
			obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			int responseCode = con.getResponseCode();
			con.getInputStream().close();
			System.out.println("Server response code:" +responseCode);
		} catch (IOException e) {
			System.err.println("No server connection!");
		}


	}
	
	
	private void feedToken(String token){
	
		
		if(command.matches("ATTACK") && token.matches(alpha)){
			command += " " +token;
		}else if(command.matches("ATTACK "+alpha) && token.matches("\\d")){
			command += " " +token;
		}else if(token.matches("ATTACK")){
			command = token;
		}else if(token.matches("STOP")){
			command = token;
		}
		else{
			command = "";
		}
		
	}
	
	private String makeSendable(String cmd){
		String[] ss = cmd.split(" ");
		if(ss.length == 3){
			return player +"x" +ss[1].substring(0, 1)+ss[2];
		}
		return null;
		
	}
	
	private String getCommand(){
		if(command.matches("ATTACK "+alpha+" \\d") || command.matches("STOP"))
			return command;
		return null;
	}
	
    public static void main(String[] args) throws Exception {
    	
    		System.out.println("arguments: player serverUrl"); 
                                     
    		SpeechClient sc = new SpeechClient();
    		try{
    			if(args.length > 0 && args[0].toUpperCase().matches("P\\d"))
    				sc.player = args[0].toUpperCase();
    			if(args.length > 1)
    				sc.serverUrl = args[1];
    			System.out.println("Setting player to "+sc.player);
    			System.out.println("Setting server URL to " +sc.serverUrl);
    			System.out.println("Say something once to 'warm up' the system.");
    			sc.run();
    		}catch(IOException ie){
    			ie.printStackTrace();
    		}

    }
}