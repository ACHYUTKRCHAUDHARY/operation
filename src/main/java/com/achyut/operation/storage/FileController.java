package com.achyut.operation.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService storage;

    @PostMapping(value = "/api/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileStorageService.StoredFile upload(@RequestPart("file") MultipartFile file,
                                                 @RequestParam(defaultValue = "work") String category) {
        return storage.store(file, category);
    }

    @GetMapping("/files/{category}/{filename:.+}")
    public ResponseEntity<Resource> serve(@PathVariable String category, @PathVariable String filename) throws MalformedURLException {
        var path = storage.resolve(category, filename);
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"", "") + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
