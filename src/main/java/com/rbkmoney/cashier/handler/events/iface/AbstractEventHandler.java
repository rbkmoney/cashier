package com.rbkmoney.cashier.handler.events.iface;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import com.rbkmoney.geck.filter.Filter;
import com.rbkmoney.geck.filter.PathConditionFilter;
import com.rbkmoney.geck.filter.condition.IsNullCondition;
import com.rbkmoney.geck.filter.rule.PathConditionRule;

public abstract class AbstractEventHandler implements EventHandler {

    private final Filter filter;

    public AbstractEventHandler(String path) {
        this.filter = new PathConditionFilter(
                new PathConditionRule(
                        path,
                        new IsNullCondition().not()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isApplicable(InvoiceChange invoiceChange) {
        return filter.match(invoiceChange);
    }
}
