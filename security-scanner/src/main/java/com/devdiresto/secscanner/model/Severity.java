package com.devdiresto.secscanner.model;

/**
 * Nível de severidade de um achado (finding), do menos ao mais crítico.
 * A ordem dos valores reflete a prioridade de correção.
 */
public enum Severity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
