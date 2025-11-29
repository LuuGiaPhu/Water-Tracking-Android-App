package com.example.myapplication;

    public class Record {
        private int id;
        private String amount;
        private String timestamp;

        public Record(int id, String amount, String timestamp) {
            this.id = id;
            this.amount = amount;
            this.timestamp = timestamp;
        }

        public int getId() {
            return id;
        }

        public String getAmount() {
            return amount;
        }

        public String getTimestamp() {
            return timestamp;
        }
    }