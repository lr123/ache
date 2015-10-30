package focusedCrawler.target;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import focusedCrawler.util.Page;
import focusedCrawler.util.Target;

/**
 * A target repository that stores pages in the file system. Files are organized per domain,
 * one folder for each domain. Supports serialization of data different formats:
 * - Plain HTML (may lose some data, about headers)
 * - JSON
 * - CBOR
 * 
 * @author aeciosantos
 *
 */
public class FileSystemTargetRepository implements TargetRepository {

	enum DataFormat {
		FILE, JSON, CBOR
	}
	
    private static final Logger logger = LoggerFactory.getLogger(FileSystemTargetRepository.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
    
    private Path directory;
	private DataFormat dataFormat;

    public FileSystemTargetRepository(String directory, DataFormat dataFormat) {
		this(Paths.get(directory), dataFormat);
    }
    
    public FileSystemTargetRepository(Path directory, DataFormat dataFormat) {
    	File fileDir = directory.toFile();
    	if(!fileDir.exists()) {
    		fileDir.mkdirs();
    	}
        this.directory = directory;
        this.dataFormat = dataFormat;
    }

    public boolean insert(Target target, int counter) {
        return insert(target);
    }

    public boolean insert(Target target) {
        try {
            String id = target.getIdentifier();
            URL url = new URL(id);
            String host = url.getHost();

            Path hostPath = directory.resolve(URLEncoder.encode(host, "UTF-8"));

            File hostDirectory = hostPath.toFile();
            if (!hostDirectory.exists()) {
                hostDirectory.mkdirs();
            }

            Path filePath = hostPath.resolve(URLEncoder.encode(id, "UTF-8"));
            
            switch(dataFormat) {
            	case FILE:
            	{
	            	try (PrintStream fileStream = new PrintStream(filePath.toFile())) {
	            	    fileStream.print(target.getSource());
	            	}
	            	break;
            	}
            	case JSON:
            	{
            		TargetModelJson targetModel = new TargetModelJson((Page) target);
            		jsonMapper.writeValue(filePath.toFile(), targetModel);
            		break;
            	}
            	case CBOR:
            	{
            		TargetModel targetModel = new TargetModel("", "", url, target.getSource());
            		cborMapper.writeValue(filePath.toFile(), targetModel);
            		break;
            	}
            }
        } catch (IOException e) {
            logger.error("Failed to store object in repository.", e);
        }
        
        return false;
    }

}
