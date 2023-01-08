package com.trodix.signature.persistance.mybatis.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import com.trodix.signature.domain.entity.SignTaskStatus;

public class TaskStatusTypeHandler extends BaseTypeHandler<SignTaskStatus> {

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int i, final SignTaskStatus parameter, final JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public SignTaskStatus getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        return SignTaskStatus.fromValue(rs.getString(columnName));
    }

    @Override
    public SignTaskStatus getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return SignTaskStatus.fromValue(rs.getString(columnIndex));
    }

    @Override
    public SignTaskStatus getNullableResult(final CallableStatement cs, final int columnIndex) throws SQLException {
        return SignTaskStatus.fromValue(cs.getString(columnIndex));
    }

}
