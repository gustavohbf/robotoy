/*******************************************************************************
 * Copyright 2016 See https://github.com/gustavohbf/robotoy/blob/master/AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.guga.robotoy.rasp.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Utilities for executing system operation application and
 * interacting with them.
 * 
 * @author Gustavo Figueiredo
 *
 */
public class ProcessUtils {
	
	public static final String DEFAULT_ENCODING = "UTF-8";

	public static final String DEFAULT_ENCODING_DOS = "Cp850";

	/**
	 * Execute command line and return the results.
	 */
	public static int execute(File initialDir,Appendable results,String encoding,String... args) throws Exception
	{
		Process proc;
		if (initialDir==null)
			proc = Runtime.getRuntime().exec(args,null);
		else
			proc = Runtime.getRuntime().exec(args,null,initialDir);
		ByteArrayOutputStream buffer = (results==null)?null:new ByteArrayOutputStream();
		PrintStream redirect_output = (results==null)?null:new PrintStream(buffer);
		StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(),redirect_output);
		StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(),redirect_output);
		errorGobbler.start();
		outputGobbler.start();
		int ret = proc.waitFor();
		while (!outputGobbler.isDone() || !errorGobbler.isDone()) {
			Thread.sleep(100);
		}
		if (results!=null) {
			redirect_output.flush();
			results.append(new String(buffer.toByteArray(),encoding));
		}
		
		return ret;
	}
	

	private static class StreamGobbler extends Thread
	{
		PrintStream redirect_output;
	    InputStream is;
	    boolean done;
	    
	    StreamGobbler(InputStream is, PrintStream redirect_output)
	    {
	        this.is = is;
	        this.redirect_output = redirect_output;
	        setDaemon(true);
	    }
	    
	    public void run()
	    {
	        try
	        {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ( (line = br.readLine()) != null) {
	            	if (redirect_output!=null)
	            		redirect_output.println(line);
	            }
	            if (redirect_output!=null) {
	            	redirect_output.flush();
	            }
	        } 
	        catch (IOException ioe)
	        {
	        	if (redirect_output!=null)
	            	ioe.printStackTrace(redirect_output);
	        }
	        finally {
	            done = true;
	        }
	    }
	    
	    public boolean isDone() {
	    	return done;
	    }
	}

}
