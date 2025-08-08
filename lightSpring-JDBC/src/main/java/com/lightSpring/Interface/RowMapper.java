package com.lightSpring.Interface;


import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.annotation.Nullable;

@FunctionalInterface
public interface RowMapper<T> {

    @Nullable
    T mapRow(ResultSet rs, int rowNum) throws SQLException;

}