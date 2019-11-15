package io.crdb.cdc.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.management.timer.Timer;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class Producer implements ApplicationRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String INSERT = "INSERT INTO source_table (id, balance, created_timestamp) VALUES (?, ?, ?)";
    private static final String UPDATE = "UPDATE source_table SET balance = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM source_table WHERE id = ?";

    private final DataSource dataSource;

    @Autowired
    public Producer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        log.info("sleeping for 1 minute before executing sql statements on the 'source` database...");

        Thread.sleep(Timer.ONE_MINUTE);

        int globalOperationCounter = 1;

        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ids.add(UUID.randomUUID());
        }

        int insertCounter = 1;

        for (UUID idToInsert : ids) {

            try (final Connection connection = dataSource.getConnection();
                 final PreparedStatement statement = connection.prepareStatement(INSERT)) {

                statement.setObject(1, idToInsert);
                statement.setInt(2, insertCounter);
                statement.setTimestamp(3, Timestamp.from(Instant.now()));

                statement.executeUpdate();

                log.debug("operation {}: inserted record with id {}", globalOperationCounter, idToInsert);

                insertCounter++;
                globalOperationCounter++;
            }

            Thread.sleep(Timer.ONE_SECOND * 10);
        }

        UUID idToUpdate = ids.get(0);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(UPDATE)) {

            statement.setInt(1, 99);
            statement.setObject(2, idToUpdate);
            statement.executeUpdate();

            log.debug("operation {}: updated record with id {}", globalOperationCounter, idToUpdate);

            globalOperationCounter++;
        }

        Thread.sleep(Timer.ONE_SECOND * 10);

        UUID idToDelete = ids.get(1);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(DELETE)) {

            statement.setObject(1, idToDelete);
            statement.executeUpdate();

            log.debug("operation {}: deleted record with id {}", globalOperationCounter, idToDelete);
        }

        log.info("sleeping for 1 minute before shutting down Producer service...");
        Thread.sleep(Timer.ONE_MINUTE);
    }
}
