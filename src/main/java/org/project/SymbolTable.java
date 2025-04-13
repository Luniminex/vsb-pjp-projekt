package org.project;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;


/**
 * Jednoduchá symbolová tabulka s podporou scope (rozsahu platnosti).
 * Používá zásobník map pro reprezentaci vnořených scope.
 */
class SymbolTable {
    // Zásobník map, kde každá mapa reprezentuje jeden scope (název -> typ)
    private final Deque<Map<String, Type>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        // Na začátku vytvoříme globální scope
        pushScope();
    }

    /** Vytvoří nový (vnořený) scope. */
    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    /** Odstraní aktuální (nejvnitřnější) scope. */
    public void popScope() {
        // Zabráníme odstranění globálního scope
        if (scopes.size() > 1) {
            scopes.pop();
        } else {
            System.err.println("Warning: Attempted to pop the global scope.");
        }
    }

    /**
     * Přidá symbol (proměnnou) do aktuálního scope.
     * @param name Název proměnné.
     * @param type Typ proměnné.
     * @return true, pokud bylo přidání úspěšné, false, pokud proměnná v tomto scope již existuje.
     */
    public boolean add(String name, Type type) {
        // Získáme aktuální (nejvrchnější) scope
        Map<String, Type> currentScope = scopes.peek();
        if (currentScope == null) {
            // Toto by nemělo nastat, pokud vždy existuje globální scope
            throw new IllegalStateException("Symbol table scope stack is empty.");
        }
        // Zkontrolujeme, zda proměnná již v tomto scope není
        if (currentScope.containsKey(name)) {
            return false; // Chyba: Redeclarace v tomto scope
        }
        currentScope.put(name, type);
        return true;
    }

    /**
     * Vyhledá typ symbolu (proměnné) procházením scope od aktuálního k vnějším.
     * @param name Název proměnné.
     * @return Typ proměnné nebo null, pokud nebyla nalezena.
     */
    public Type lookup(String name) {
        // Procházíme scope od nejvnitřnějšího (vrcholu zásobníku) k vnějším
        for (Map<String, Type> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return null; // Symbol nenalezen v žádném scope
    }

    /**
     * Zkontroluje, zda symbol existuje POUZE v aktuálním (nejvnitřnějším) scope.
     * Používá se pro kontrolu redeklarace.
     * @param name Název proměnné.
     * @return true, pokud existuje v aktuálním scope, jinak false.
     */
    public boolean existsInCurrentScope(String name) {
        Map<String, Type> currentScope = scopes.peek();
        if (currentScope == null) {
            throw new IllegalStateException("Symbol table scope stack is empty.");
        }
        return currentScope.containsKey(name);
    }
}
