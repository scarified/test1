import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class WordPairs
{
	static final int MIN_PREFIX_LEN = 3;
	static final int MAX_PREFIX_LEN = 4;
	static final int MIN_SEGMENT_SIZE = 5;
	
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
		else if (s.length() == 1)
		{
			return s.substring(0, 1).toUpperCase();
		}
		else
		{
			return s;
		}
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
			int numexpanded = numWords*(MAX_PREFIX_LEN+1);
			
			String[] expanded = new String[numexpanded];
			for(int i = 0; i < numWords; i++)
			{
				int len = words[i].length();
				
				for(int j = 0; j < MAX_PREFIX_LEN; j++)
				{
					if(len > (j+1))	expanded[(i*(MAX_PREFIX_LEN+1)) + j] = uc(words[i].substring(0,(j+1)));
				}
				expanded[(i*(MAX_PREFIX_LEN+1)) + MAX_PREFIX_LEN] = uc(words[i]);				
			}
					
			String[] next = new String[numexpanded];	
			for( int i = 0; i < numexpanded; i++ )
			{	
				int zerolen = expanded[0] == null?0:expanded[0].length();
				for( int j = 1; j < numexpanded; j++ )
				{		
					int jlen = expanded[j] == null?0:expanded[j].length();
					StringBuffer sb = new StringBuffer();
					if( expanded[j] != null &&  // The original word was shorter than MAX_PREFIX_LEN
						expanded[0] != null &&  // The original word was shorter than MAX_PREFIX_LEN
						!expanded[j].startsWith(expanded[0]) && 
						!expanded[0].startsWith(expanded[j]) &&
						jlen >= MIN_PREFIX_LEN && 
						zerolen >= MIN_PREFIX_LEN )
					{
						sb.append(expanded[0]);
						sb.append(expanded[j]);
						
						if( (zerolen + jlen) > MIN_SEGMENT_SIZE)	
						{
							System.out.println(sb.toString());
						}
					}
					next[j-1] = expanded[j];
				}
				next[numexpanded-1] = expanded[0];				
				System.arraycopy(next, 0, expanded, 0, numexpanded);
			}			
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
