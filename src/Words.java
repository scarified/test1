import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Words
{
	static final int PREFIX_LEN = 3;
	static final int MAX_LEN = 20;
	static final int CONCAT_SIZE = 20;
	
	
	static ArrayList<String> used = new ArrayList<String>();
	
	static String uc(String s)
	{
		if (s == null)
		{
			return null;
		}
		else if (s.length() > 1)
		{
			String first = s.substring(0, 1).toUpperCase();
			String rest = s.substring(1).toLowerCase();
			return first + rest;
		}
		else if (s.length() > 1)
		{
			return s.substring(0, 1).toUpperCase();
		}
		else
		{
			return s;
		}
	}

	private static boolean mix(String[] arr, int index)
	{
		boolean ret = true;
		
		int maxlen = Math.min(arr.length, 2);
		
		if (index >= arr.length - 1)
		{ 		
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < maxlen - 1; i++)
			{
				if( true /*sb.length() + arr[i].length() <= MAX_LEN*/ )
				{
					sb.append(uc(arr[i]));
				}
				else
				{
					ret = false;
					break; // Complete list - may take a long time
					//return ret; // Shortcut for a smaller result set
				}
			}
			if (maxlen > 0 )
			{			
				if( true /* sb.length() + arr[maxlen - 1].length() <= MAX_LEN*/ )
				{
					sb.append(uc(arr[maxlen	 - 1]));
				}
				else
				{
					//ret = false;
					//return ret; // Shortcut for a smaller result set
				}
			}
			
			if( sb.length() > 0 )
			{
				String s = sb.toString();
				if( !used.contains(s) )
				{
					used.add(s);
					System.out.println(s);
					//System.out.flush();
				}
			}
			
			return ret;
		}

		for (int i = index; i < arr.length; i++)
		{ 
			String s = arr[index];
			arr[index] = arr[i];
			arr[i] = s;
		
			if(mix(arr, index + 1) == false)
			{
				break; // hangs
				//return false; // skips stuff
			}
						
			s = arr[index];
			arr[index] = arr[i];
			arr[i] = s;
		}
		
		return true;
	}
	
	public static String[] readFile(String f) throws IOException
	{
		ArrayList<String> words = new ArrayList<String>();		
		
		BufferedReader reader;
		reader = new BufferedReader(new FileReader(f));
		String line = reader.readLine();
		while (line != null) 
		{
			String s = line.trim();
			if(s != null && s.length() > 0)
			{
				words.add(line.trim());
			}
			line = reader.readLine();
		}
		reader.close();
		
		String[] array = new String[words.size()];
		return words.toArray(array);
	}

	public static void main(String[] args)
	{
		try
		{
			String[] words;			
			
			if( args.length < 1 )
			{
				System.err.println("Usage: java -jar words.jar [-f input_file | word1 word2 ...]");
				return;
			}
			else if( args.length > 1 && "-f".equalsIgnoreCase(args[0]) )
			{
				words = readFile(args[1]);
			}
			else
			{
				words = args;
			}
			
			int numWords = words.length;
					
			String[] bar = new String[numWords];
			for(int z = 0; z < numWords; z++)
			{				
				for(int x = 0; x < numWords; x++)
				{
					if( x < numWords-1 )
					{
						bar[x] = words[x+1];
					}
					else
					{
						bar[x] = words[0];
					}
				}
				words = bar;
				
							
				for(int l = numWords ; l > 1; l--)
				{			
					int destlen = Math.min(CONCAT_SIZE, l);
					
					for(int i = 0; i < 1; i++)
					{
						
						String[] foo = new String[destlen];
						System.arraycopy(bar, 0, foo, 0, destlen);
						
						String s = foo[i];		
						if( s != null )
						{
							int slen = Math.min((PREFIX_LEN+1), s.length());
							
							for( int j = 0; j < slen; j++ )
							{
								if( j > 0 )
								{
									foo[i] = s.substring(0, j);
								}
								else
								{
									foo[i] = s;
								}
								mix(foo, 0);
							}
						}
					}
				}		
			}	
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
