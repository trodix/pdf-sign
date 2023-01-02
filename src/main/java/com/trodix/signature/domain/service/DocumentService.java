package com.trodix.signature.domain.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.trodix.signature.domain.model.Document;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DocumentService {

    @Value("${app.signed-file-destination}")
    private String fileStoragePath;

    public static Path storeFile(final String signedFileDestination, final MultipartFile fileUpload, final UUID documentId) throws IOException {
        final String docExt = FilenameUtils.getExtension(fileUpload.getOriginalFilename());
        final String newName = documentId.toString() + "." + docExt;
        final Path dest = Paths.get(signedFileDestination, newName);
        fileUpload.transferTo(dest);
        return dest;
    }

    public Path storeFile(final MultipartFile fileUpload, final UUID documentId) throws IOException {
        return storeFile(fileStoragePath, fileUpload, documentId);
    }

    public Path signedDocumentToPath(final Document document) {
        final String ext = FilenameUtils.getExtension(document.getOriginalFileName());
        final String signedDocumentName = document.getDocumentId().toString() + "-signed" + "." + ext;
        return Paths.get(fileStoragePath, signedDocumentName);
    }

    public Path documentToPath(final Document document) {
        final String ext = FilenameUtils.getExtension(document.getOriginalFileName());
        final String signedDocumentName = document.getDocumentId().toString() + "." + ext;
        return Paths.get(fileStoragePath, signedDocumentName);
    }

    public void deleteSignedDocument(final Document document) {
        try {
            final File signedDocument = getDocument(document);
            Files.delete(signedDocument.toPath());
        } catch (final IOException e) {
            log.error("Error while deleting signed document {}", document.getDocumentId(), e);
        }
    }

    public File getDocument(final Document documentModel) {
        File document;

        try {
            final Path path = documentToPath(documentModel);
            document = path.toFile();
            return document;
        } catch (final RuntimeException e) {
            log.error("Error while reading the document from id: {}", documentModel.getDocumentId());
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public File getSignedDocument(final Document documentModel) {
        File document;

        try {
            final Path path = signedDocumentToPath(documentModel);
            document = path.toFile();
            return document;
        } catch (final RuntimeException e) {
            log.error("Error while reading the document from id: {}", documentModel.getDocumentId());
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    public static File multipartFileToFile(final MultipartFile file) throws IOException {

        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is a mandatory parameter");
        }

        final File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();

        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }

        return convFile;
    }

}
