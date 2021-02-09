package com.example.demo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.http.MediaType;

public class Pack
{
    public void start(String project, ZipArchiveOutputStream zos) throws IOException, InterruptedException
    {
        /* Additional setup goes here */
        read(project, zos);
    }

    public void read(String project, ZipArchiveOutputStream zos) throws IOException, InterruptedException
    {
        String line, path;
        int type, permissions;
        long chunkSize;
        boolean endOfAllFound = false;
        InputStream is = ((CMAPI_PERMISSIONS_CALL) ? getStream(project)
                : new FileInputStream(new File(project).getCanonicalPath()));               // Make CMAPI GET request for chunked input stream of project

        /* Start parsing the file */
        line = readLine(is);                                                                // Read the first line (chunksize)
        while (!endOfAllFound)                                                              // While no double 0 is found conti. parsing
        {
            // Path*************************************************************************
            chunkSize = Long.parseLong(line, 16);                                           // Get length of path name
            if (chunkSize == 0)                                                             // Validate that double 0 is not found
            {
                endOfAllFound = true;
                break;
            }
            path = getBytes(is, chunkSize, null);                                           // Read the bytes
            readNewLine(is);                                                                // Read new line character
            readEndBit(is);                                                                 // Read the ending bit
            
            // Type*************************************************************************
            line = readLine(is);                                                            // Read the first line (chunksize)
            chunkSize = Long.parseLong(line, 16);                                           // Get length of file type (always 1)
            type = Integer.parseInt(getBytes(is, chunkSize, null));                         // Read the bytes
            if (!validateType(type))
            {
                throw new IllegalArgumentException("Unknown type " + type);                 // Make invalid type exception
            }
            readNewLine(is);                                                                // Read new line character
            readEndBit(is);                                                                 // Read the ending bit

            // Permissions*******************************************************************
            line = readLine(is);                                                            // Read the first line (chunksize)
            chunkSize = Long.parseLong(line, 16);                                           // Get length of permissions (always 4)
            permissions = Integer.parseInt(getBytes(is, chunkSize, null), 8);               // Read the bytes (octal)
            if (!validatePermissions(permissions))
            {
                throw new IllegalArgumentException("Unknown permission " + permissions);    // Make invalid permission exception
            }
            readNewLine(is);                                                                // Read new line character
            readEndBit(is);                                                                 // Read the ending bit

            // Content************************************************************************
            // Absolutely sure you have a valid symlink or file and you know which it is
            line = readLine(is);                                                            // Read the first line (chunksize)
            chunkSize = Long.parseLong(line, 16);                                           // Get length of the content
            zipIt(zos, is, type, permissions, path, chunkSize);
            zos.closeArchiveEntry();
           
            line = readLine(is);                                                            // Read the first line (chunksize)
        }

        /* Clean up */
        is.close();
    }

    /*
     * Helper Functions
     ***********************************************************************/
    private void zipIt(ZipArchiveOutputStream zos, InputStream is, int type, int permissions, String path,
            long chunkSize) throws IOException, InterruptedException
    {
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(path);               // Organizes everything in the zip file based on the path
        if (type == TYPEFILE)
        {
            zipEntry.setUnixMode(UnixStat.FILE_FLAG | permissions);         // To force type = UNIX
            zos.putArchiveEntry(zipEntry);                                  // Add the zip entry to the archive
            getChunkedBytes(is, chunkSize, zos);                            // Write to zip archive stream. a better solution than IOUtils.copyLarge(is, zos, 0, chunkSize)
            zos.flush();
        }
        else if (type == TYPELINK)
        {
            zipEntry.setUnixMode(UnixStat.LINK_FLAG);                       // This is 1 of 2 magic lines
            String linkedFile = getBytes(is, (int) chunkSize, null);      
            AsiExtraField asi = new AsiExtraField();                        // Make an extra field
            asi.setLinkedFile(linkedFile);                                  // Recreate symlink
            zipEntry.addAsFirstExtraField(asi);                             // Attach field to entry
            zos.putArchiveEntry(zipEntry);                                  // Add the zip entry to the archive
            zos.write(linkedFile.getBytes());                               // This is 2 of 2 magic lines
        }
        else
        {
            throw new IllegalArgumentException("Unknown type " + type + "."); // File didn't pass a 0 or 1
        }
    }

    private void readEndBit(InputStream is) throws IOException, InterruptedException
    {
        String line = readLine(is);
        if (!"0".contentEquals(line)) // Make sure end of chunk entry is found
        {
            throw new IOException("Did not find end.");
        }
    }

    private static boolean validateType(int type)
    {
        return (type == TYPEFILE || type == TYPELINK); // Should only be 0 or 1
    }

    private static boolean validatePermissions(int permissions)
    {
        return (permissions <= 511); // Should be replaced by something more robust
    }
    
    private void getChunkedBytes(InputStream is, long chunkSize, OutputStream os) throws IOException, InterruptedException
    {
    	while( chunkSize > 0 )
    	{
	    	getBytes(is, chunkSize, os);
	    	readNewLine(is);
	        String line = readLine(is);                                                            // Read the first line (chunksize)
	        chunkSize = Long.parseLong(line, 16); 
    	}
    }

    @SuppressWarnings("resource")
    private String getBytes(InputStream is, long chunkSize, OutputStream os) throws IOException, InterruptedException
    {
        if( chunkSize == 0 )
        {
            return null;
        }
        
        if (os == null)
        {
            os = new ByteArrayOutputStream((int)chunkSize);                                                                   // Only used for non-zip entries
        }
        
        long remainingBytes = chunkSize;                                                                        // Counter
        long smallest = Math.min(chunkSize, (long) available(is));  // Possible bug with fis available 0
        byte[] buffer = new byte[(smallest <= (long)DEFAULT_BUFFER_SIZE) ?(int)smallest :DEFAULT_BUFFER_SIZE];  // Get the smallest buffer size

        while (remainingBytes >= 0)
        {
            int bytesRead = is.read(buffer);
            if (bytesRead >= 0)
            {
                remainingBytes -= bytesRead;
                os.write(buffer, 0, bytesRead); // Send to zip stream
                if (remainingBytes > 0)
                {
                    int available = available(is);
                                        
                    if (available < 0)
                    {
                        throw new IOException("Can't read from input stream.");
                    }
                    else if (available < remainingBytes && available <= buffer.length && available > 0)
                    {
                        buffer = new byte[available];
                    }
                    else if ((remainingBytes < buffer.length) || (remainingBytes <= available))
                    {
                        buffer = new byte[(int) remainingBytes];
                    }
                    else
                    {
                        buffer = new byte[DEFAULT_BUFFER_SIZE];
                    }
                }
                else
                {
                    return os instanceof ByteArrayOutputStream ? new String(((ByteArrayOutputStream)os).toByteArray()) : null ; // Path, type, permissions and content
                }
            }
            else
            {
                // An error has occurred if you get here (nothing was read)
                throw new IllegalStateException("No bytes were read.");
            }
        }

        throw new IllegalStateException("Chunk size of " + remainingBytes + " is invalid."); // Should never get here but needed for String return type
    }

    private void readNewLine(InputStream is) throws IOException, InterruptedException
    {
    	String s = getBytes(is, NEWLINE.length(), null);
        if( !NEWLINE.equals(s) )
        {
            throw new IOException("Did not read the new line. Got \"" + s + "\"");
        }
    }

    public String readLine(InputStream inputStream) throws IOException, InterruptedException
    {
    	/* 
    	 * MJH - this code assumes that NEWLINE is 2 bytes long, which it is, 
    	 *       but it really ought to be more generic
    	 */    	
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte b = (byte)inputStream.read();
        byte last = EOF;
        while ( b != EOF )
        {               	
        	if ( last != EOF )
        	{
	        	if (NEWLINE.equals(new String( new byte[] {last, b} )))
				{
	        		break;
				}
	        	else
	        	{            	
	                byteArrayOutputStream.write(last); 
	        	}	        	
        	}
        	last =  b;
        	b = (byte)inputStream.read();
        }
        
        if (byteArrayOutputStream.size() == 0)
        {
            return null;
        }
        
        return byteArrayOutputStream.toString("UTF-8");
    }
    
    private int available(InputStream is) throws IOException, InterruptedException
    {
    	int retries = 0;
    	int available = is.available();
        while (available == 0 && retries < MAX_RETRIES)
        {
        	Thread.sleep(SLEEP_PERIOD);
        	available = is.available();
        	retries++;
        }
        return available;
    }

    public static InputStream getStream(String fileName) throws IOException
    {
        // TODO - Replace this with a real CMAPI call
        URL url = new URL("http://localhost:9000/stream?fileName=" + fileName);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE);
        con.setDoInput(true);
        return con.getInputStream();
    }

    /*
     * Constants
     *******************************************************************************/
    public static final int TYPEFILE = 0;
    public static final int TYPELINK = 1;
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static final int SLEEP_PERIOD = 100; // milliseconds
    private static final int MAX_RETRIES = 10;
    private static final String NEWLINE = "\r\n"; // Not platform specific because we own the stream format
    private static final boolean CMAPI_PERMISSIONS_CALL = true;
    private static final int EOF = -1;
}
