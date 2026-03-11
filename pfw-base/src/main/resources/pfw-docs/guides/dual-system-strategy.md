# Dual-System-Strategie: Claude (Fork A) + Gemini (Fork B)

## Prinzip

Architektur zentral entscheiden (hier, in Claude).
Implementation dezentral (je Fork).
Transport: ADR-Dokumente (kein Source Code zwischen Forks).

## ADR-Format

```
Kontext:         Warum diese Entscheidung?
Entscheidung:    Was wird gebaut?
Neue Abstraktionen: Welche Interfaces/Klassen?
Regeln:          Was ist erlaubt, was nicht?
Sequenz:         In welcher Reihenfolge?
Verifikation:    Checkliste — wann ist es fertig?
```

## Erstellte ADRs

| ADR | Thema | Status |
|-----|-------|--------|
| ADR-001 | ContextProviderResolver | Implementiert |
| ADR-002 | InstanceProvider (provide + extract) | Implementiert |
| ADR-003 | ITypeDescriptor + ValueFunctionResolverChain | Implementiert |
| ADR-004 | Source→Context Migration + Scope Provided | Akzeptiert |
