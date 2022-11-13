package com.trodix.signature.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.trodix.signature.dto.request.CreateSignTaskRequest;
import com.trodix.signature.dto.response.SignTaskResponse;
import com.trodix.signature.dto.response.SignedDocumentResponse;
import com.trodix.signature.entity.SignTaskEntity;
import com.trodix.signature.entity.SignatureHistoryElementEntity;
import com.trodix.signature.entity.SignedDocumentEntity;
import com.trodix.signature.model.SignTaskModel;
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

    @Mapping(target = "id", ignore = true)
    public SignatureHistoryElementEntity signatureHistoryElementModelToSignatureHistoryElementEntity(SignatureHistoryElementModel signatureHistoryElementModel);

    public static SignTaskModel signTaskRequestToSignTaskModel(final CreateSignTaskRequest signTaskRequest) {
        final SignTaskModel signTaskModel = new SignTaskModel();
        signTaskModel.setTmpDocument(signTaskRequest.getDocument());
        signTaskModel.setDueDate(signTaskRequest.getDueDate());
        signTaskModel.setRecipientEmail(signTaskRequest.getRecipientEmail());
        signTaskModel.setSenderEmail(signTaskRequest.getSenderEmail());

        return signTaskModel;
    }

    @Mapping(target = "id", ignore = true)
    public SignTaskEntity signTaskModelToSignTaskEntity(SignTaskModel signTaskModel);

    @Mapping(target = "tmpDocument", ignore = true)
    public SignTaskModel signTaskEntityToSignTaskModel(SignTaskEntity signTaskEntity);

    public SignTaskResponse signTaskModelToCreateSignTaskResponse(SignTaskModel signTaskModel);

}
