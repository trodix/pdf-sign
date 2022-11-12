package com.trodix.signature.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.trodix.signature.dto.response.SignResponse;
import com.trodix.signature.entity.SignatureDocumentEntity;
import com.trodix.signature.model.SignedDocumentModel;

@Mapper(componentModel = "cdi")
public interface SignatureMapper {

    @Mapping(target = "id", ignore = true)
    public SignatureDocumentEntity signedDocumentModelToSignatureDocumentEntity(SignedDocumentModel signedDocumentModel);

    public SignedDocumentModel signatureDocumentEntityToSignedDocumentModel(SignatureDocumentEntity signatureDocumentEntity);

    public List<SignedDocumentModel> signatureDocumentEntityListToSignedDocumentModelList(List<SignatureDocumentEntity> signatureDocumentEntities);

    public SignResponse signedDocumentModelToSignResponse(SignedDocumentModel signedDocumentModel);

    public List<SignResponse> signedDocumentModelListToSignResponseList(List<SignedDocumentModel> models);

}
