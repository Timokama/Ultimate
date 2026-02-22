#!/usr/bin/env python3
"""
Migration script to change preferred_schedule from DATE to VARCHAR
"""
import psycopg2

def migrate():
    conn = psycopg2.connect(
        host="localhost",
        database="postgres",
        user="postgres",
        password="postgres"
    )
    conn.autocommit = True
    cur = conn.cursor()
    
    try:
        cur.execute("ALTER TABLE applications ALTER COLUMN preferred_schedule TYPE VARCHAR(50)")
        print("SUCCESS: preferred_schedule changed from DATE to VARCHAR(50)")
    except Exception as e:
        print(f"ERROR: {e}")
    finally:
        cur.close()
        conn.close()

if __name__ == "__main__":
    migrate()
