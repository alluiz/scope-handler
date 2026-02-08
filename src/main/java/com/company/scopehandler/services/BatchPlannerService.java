package com.company.scopehandler.services;

import com.company.scopehandler.domain.Operation;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class BatchPlannerService {
    public BatchPlan plan(List<String> clients, List<String> scopes) {
        if (clients.isEmpty() || scopes.isEmpty()) {
            throw new IllegalArgumentException("clients and scopes must not be empty");
        }
        long total = (long) clients.size() * (long) scopes.size();
        return new BatchPlan(new OperationIterable(clients, scopes), total);
    }

    private static final class OperationIterable implements Iterable<Operation> {
        private final List<String> clients;
        private final List<String> scopes;

        private OperationIterable(List<String> clients, List<String> scopes) {
            this.clients = clients;
            this.scopes = scopes;
        }

        @Override
        public Iterator<Operation> iterator() {
            return new Iterator<>() {
                private int clientIndex = 0;
                private int scopeIndex = 0;

                @Override
                public boolean hasNext() {
                    return clientIndex < clients.size() && scopeIndex < scopes.size();
                }

                @Override
                public Operation next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    Operation op = new Operation(clients.get(clientIndex), scopes.get(scopeIndex));
                    scopeIndex++;
                    if (scopeIndex >= scopes.size()) {
                        scopeIndex = 0;
                        clientIndex++;
                    }
                    return op;
                }
            };
        }
    }
}
