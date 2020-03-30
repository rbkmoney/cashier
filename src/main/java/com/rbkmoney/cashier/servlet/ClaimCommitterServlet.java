package com.rbkmoney.cashier.servlet;

import com.rbkmoney.damsel.claim_management.ClaimCommitterSrv;
import com.rbkmoney.woody.thrift.impl.http.THServiceBuilder;
import lombok.RequiredArgsConstructor;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/claim-committer")
@RequiredArgsConstructor
public class ClaimCommitterServlet extends GenericServlet {

    private final ClaimCommitterSrv.Iface claimCommitterServerHandler;
    private Servlet servlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servlet = new THServiceBuilder().build(ClaimCommitterSrv.Iface.class, claimCommitterServerHandler);
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        servlet.service(request, response);
    }
}
