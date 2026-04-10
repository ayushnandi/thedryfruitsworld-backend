package com.thedryfruitsworld.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies the handle_new_user trigger fix on startup.
 * Adds search_path and exception handler so trigger failures never block signup.
 * Safe to run repeatedly — uses CREATE OR REPLACE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseTriggerMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        applyTriggerFix();
    }

    private void applyTriggerFix() {
        try {
            // Step 1: recreate the function with search_path + exception handler
            jdbcTemplate.execute("""
                    CREATE OR REPLACE FUNCTION public.handle_new_user()
                    RETURNS trigger
                    SECURITY DEFINER
                    SET search_path = public
                    LANGUAGE plpgsql AS $$
                    BEGIN
                      INSERT INTO public.profiles (id, full_name, phone)
                      VALUES (
                        NEW.id,
                        NEW.raw_user_meta_data->>'full_name',
                        NEW.raw_user_meta_data->>'phone'
                      )
                      ON CONFLICT (id) DO NOTHING;
                      RETURN NEW;
                    EXCEPTION WHEN OTHERS THEN
                      RETURN NEW;
                    END;
                    $$
                    """);

            // Step 2: recreate the trigger on auth.users
            jdbcTemplate.execute(
                    "DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users");

            jdbcTemplate.execute("""
                    CREATE TRIGGER on_auth_user_created
                      AFTER INSERT ON auth.users
                      FOR EACH ROW EXECUTE FUNCTION public.handle_new_user()
                    """);

            log.info("Supabase handle_new_user trigger fix applied successfully");

        } catch (Exception e) {
            // Non-fatal — app still starts, but log clearly so it's visible in Railway logs
            log.error("Supabase trigger migration FAILED: {}", e.getMessage());
        }
    }
}
