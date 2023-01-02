// package com.trodix.signature.mapper;

// import java.util.List;
// import org.mapstruct.Mapper;
// import org.mapstruct.Mapping;
// import com.trodix.signature.domain.entity.DocumentEntity;
// import com.trodix.signature.domain.model.Document;
// import com.trodix.signature.presentation.dto.response.DocumentResponse;

// @Mapper(componentModel = "spring", uses = { SignatureHistoryEntryMapper.class })
// public interface DocumentMapper {

//     @Mapping(target = "id", ignore = true)
//     public DocumentEntity documentToDocumentEntity(Document document);

//     public Document documentEntityToDocument(DocumentEntity documentEntity);

//     public List<Document> documentEntityListToDocumentList(List<DocumentEntity> signatureDocumentEntities);

//     public List<DocumentResponse> documentModelListToDocumentResponseList(List<Document> documentList);

// }
