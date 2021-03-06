package com.ming.inclination.service;

import com.ming.inclination.entity.TblDataOffsetToOrcl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class DataService {

    private final static Logger LOGGER = LogManager.getLogger(DataService.class);

    @Value("${oracleUrl}")
    private String oracleUrl;//oracle数据库地址
    @Value("${oracleUsername}")
    private String oracleUsername;//用户名
    @Value("${oraclePassword}")
    private String oraclePassword;//密码

    public void insertAll(List<TblDataOffsetToOrcl> offsetList) throws SQLException, ClassNotFoundException {
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String sql = "INSERT INTO TBL_DATA_OFFSET(ID,BRIDGE_ID,MEASURE_POINT,OFFSET,AC_TIME) VALUES(?,?,?,?,?)";

        try (Connection connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
             PreparedStatement preStat = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            TblDataOffsetToOrcl offset;
            for (int i = 0; i < offsetList.size(); i++) {
                offset = offsetList.get(i);
                preStat.setString(1, offset.getId());
                preStat.setString(2, offset.getBridgeId());
                preStat.setString(3, offset.getMeasurePoint());
                preStat.setDouble(4, offset.getOffset());
                preStat.setTimestamp(5, offset.getAcTime());
                preStat.addBatch();
            }
            LOGGER.debug(preStat.toString());
            preStat.executeBatch();
            connection.commit();
            LOGGER.debug("存入ORACLE数据库: " + offsetList.size());
        }

    }

}
