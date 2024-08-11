package io.github.sergeiionin.contractsregistrator
package http.client

import org.http4s.Request
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Header, Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci.CIString
import org.http4s.headers.{Accept, Authorization}
import org.http4s.Credentials.Token
import org.http4s.AuthScheme.Bearer
import org.http4s.headers.Authorization.given
import org.http4s.headers.Accept.given

object ClientUtils:
  private val acceptHeader = Header.Raw(CIString("Accept"), "application/vnd.github.v3+json")
  private val gitHubApiVersionHeader = Header.Raw(CIString("X-GitHub-Api-Version"), "2022-11-28")
  private def authHeader(token: String) = Authorization(Token(Bearer, token))
  private val contentType = Accept(MediaType.application.json)
  
  def getRequest[F[_]](uri: Uri, token: Option[String]): Request[F] =
    val headers = token match
      case Some(t) => Headers(authHeader(t), acceptHeader, gitHubApiVersionHeader, contentType)
      case None => Headers(acceptHeader, gitHubApiVersionHeader, contentType)
    
    Request[F](
      Method.GET, uri,
      headers = headers
    )

  def postRequest[F[_], T](uri: Uri, token: Option[String], entity: T)(using EntityEncoder[F, T]): Request[F] =
    val headers = token match
      case Some(t) => Headers(authHeader(t), acceptHeader, gitHubApiVersionHeader, contentType)
      case None => Headers(acceptHeader, gitHubApiVersionHeader, contentType)
    
    Request[F](
      Method.POST, uri,
      headers = headers)
      .withEntity(entity)