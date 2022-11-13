package com.trodix.signature.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.trodix.signature.dto.response.SignedDocumentResponse;
import com.trodix.signature.entity.SignatureHistoryElementEntity;
import com.trodix.signature.entity.SignedDocumentEntity;
import com.trodix.signature.model.SignatureHistoryElementModel;
import com.trodix.signature.model.SignedDocumentModel;

@Mapper(componentModel = "cdi")
public interface SignatureMapper {

    @Mapping(target = "id", ignore = true)
    public SignedDocumentEntity signedDocumentModelToSignedDocumentEntity(SignedDocumentModel signedDocumentModel);

    public SignedDocumentModel signatureDocumentEntityToSignedDocumentModel(SignedDocumentEntity signatureDocumentEntity);

    public List<SignedDocumentModel> signatureDocumentEntityListToSignedDocumentModelList(List<SignedDocumentEntity> signatureDocumentEntities);

    public SignedDocumentResponse signedDocumentModelToSignedDocumentResponse(SignedDocumentModel signedDocumentModel);

    public List<SignedDocumentResponse> signedDocumentModelListToSignResponseList(List<SignedDocumentModel> models);

    public SignatureHistoryElementModel signatureHistoryElementEntityToSignatureHistoryElementModel(
            SignatureHistoryElementEntity signatureHistoryElementEntity);

    public SignatureHistoryElementEntity signatureHistoryElementModelToSignatureHistoryElementEntity(SignatureHistoryElementModel signatureHistoryElementModel);

}
