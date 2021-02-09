package pojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class Zip
{
public static void main(String args[])
	{

		try
		{

			FileOutputStream fos = new FileOutputStream("out.zip");
			ZipArchiveOutputStream zipOS = new ZipArchiveOutputStream(fos);		

			String[] pathnames;
			File f = new File(args[0]);
			pathnames = f.list();
			String path = f.getPath();

			for (String pathname : pathnames)
			{
				System.out.println(path + File.separator + pathname);
				writeToZipFile(path + File.separator + pathname, zipOS);
			}

			zipOS.finish();
			zipOS.close();
			fos.close();

		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public static void writeToZipFile(String path, ZipArchiveOutputStream zipStream)
			throws FileNotFoundException, IOException
	{
		File aFile = new File(path);
		FileInputStream fis = new FileInputStream(aFile);
		ZipArchiveEntry zipEntry = new ZipArchiveEntry(path);
		
		if (Files.isSymbolicLink(Paths.get(path)))
		{
			zipEntry.setUnixMode(UnixStat.LINK_FLAG);  // This is 1 of 2 magic lines
			AsiExtraField asi = new AsiExtraField();
			Path s = Files.readSymbolicLink(Paths.get(path));
			s = s.normalize();
			asi.setLinkedFile(s.toString());
			zipEntry.addAsFirstExtraField(asi);
			zipStream.putArchiveEntry(zipEntry);
			zipStream.write(s.toString().getBytes());  // This is 2 of 2 magic lines
		} 
		else if( !aFile.isDirectory() )
		{
			Set<PosixFilePermission> filePerm = Files.getPosixFilePermissions(Paths.get(path), LinkOption.NOFOLLOW_LINKS);
			zipEntry.setUnixMode(translatePosixPermissionToMode(filePerm)); // To force type = UNIX
			zipStream.putArchiveEntry(zipEntry);
			IOUtils.copy(fis, zipStream);
		}
		else
		{
			// it's a directory, iterate or something
		}

		zipStream.closeArchiveEntry();
		fis.close();
	}

	public static int translatePosixPermissionToMode(Set<PosixFilePermission> permission)
	{
		int mode = 0;
		for (PosixFilePermission action : PosixFilePermission.values())
		{
			mode = mode << 1;
			mode += permission.contains(action) ? 1 : 0;
		}
		return UnixStat.FILE_FLAG + mode;
	}
}
