package no.nav.metrics;

interface Metric {
    void addFieldToReport(String fieldName, Object value);
    void report();
}
