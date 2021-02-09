package com.example.demo;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * What does this do?
 * This controller is used to manage REST API end points, this where URLs, methods (GET, POST, PATCH etc.),
 * return types and other operations are set. 
 * */
@RestController                                                                                                                 //  Tag telling Spring Boot that this controls your REST end points
public class SbPhonyPacketsController {
    
    @ModelAttribute
    @RequestMapping(value = "/zipdownloadall", produces="application/zip", method = RequestMethod.GET)                          //  Sets a GET request mapping at http://localhost:9000/zipdownloadall
    public void zipDownloadAll(@RequestParam("fileName") String fileName, HttpServletResponse response) 
            throws IOException, InterruptedException {                                                                          //  Function that takes a HTTP response to be used for an output stream     
        ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(response.getOutputStream());                                 //  Take the response and use it as a ZIP stream    
        zipOut.setUseZip64(Zip64Mode.Always);                                                                                   //  Allows zipping of 4GB+ files
        String zipFileName = "test.zip";                                                                                        //  Name of new ZIP file
        response.setStatus(HttpServletResponse.SC_OK);                                                                          
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName); 
        Pack pack = new Pack();
        pack.start(fileName, zipOut);                                                                                           //  Function used to ZIP files to response stream
        zipOut.finish();
        zipOut.close();
    }

}
