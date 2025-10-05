package com.my.challenger.web.controllers;

import com.my.challenger.migration.QuestionBankMigrationService;
import com.my.challenger.migration.QuestionBankMigrationService.MigrationResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/migration")
@RequiredArgsConstructor
public class MigrationController {
    
    private final QuestionBankMigrationService migrationService;
    
    /**
     * Execute the migration
     * WARNING: This will restructure your database!
     */
    @GetMapping("/execute")
    public ResponseEntity<MigrationResult> executeMigration() {
        MigrationResult result = migrationService.migrateQuestionsToBank();
        return ResponseEntity.ok(result);
    }
}