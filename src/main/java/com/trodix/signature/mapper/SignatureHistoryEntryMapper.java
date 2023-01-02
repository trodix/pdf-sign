package com.trodix.signature.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.trodix.signature.domain.entity.SignatureHistoryEntryEntity;
import com.trodix.signature.domain.model.SignatureHistoryEntry;

@Mapper(componentModel = "spring")
public interface SignatureHistoryEntryMapper {

    @Mapping(target = "id", ignore = true)
    public SignatureHistoryEntryEntity signatureHistoryEntryToSignatureHistoryEntryEntity(SignatureHistoryEntry signatureHistoryEntry);

    public SignatureHistoryEntry signatureHistoryEntryEntityToSignatureHistoryEntry(SignatureHistoryEntryEntity signatureHistoryEntryEntity);

}
