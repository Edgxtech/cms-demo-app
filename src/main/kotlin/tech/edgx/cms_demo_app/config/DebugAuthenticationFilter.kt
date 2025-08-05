package tech.edgx.cms_demo_app.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.filter.OncePerRequestFilter

class DebugAuthenticationFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        println("Debug: Entering DebugAuthenticationFilter for ${request.method} ${request.requestURI}")
        println("Debug: Request Headers: ${request.headerNames.toList().associateWith { request.getHeader(it) }}")
        val authHeader = request.getHeader("Authorization")
        println("Debug: Authorization Header: $authHeader")
        val securityContext = SecurityContextHolder.getContext()
        println("Debug: SecurityContext: $securityContext")
        val authentication = securityContext.authentication
        println("Debug: Authentication: $authentication")
        if (authentication != null) {
            println("Debug: Authentication isAuthenticated: ${authentication.isAuthenticated}")
            println("Debug: Principal: ${authentication.principal}")
            println("Debug: Authorities: ${authentication.authorities}")
            if (authentication.principal is Jwt) {
                val jwt = authentication.principal as Jwt
                println("Debug: JWT Claims: ${jwt.claims}")
            }
        }
        println("Debug: Exiting DebugAuthenticationFilter")
        filterChain.doFilter(request, response)
    }
}