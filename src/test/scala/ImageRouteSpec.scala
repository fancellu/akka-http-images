import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, IOException}

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import javax.imageio.ImageIO
import org.scalatest.{Matchers, WordSpec}

class ImageRouteSpec extends WordSpec with Matchers with ScalatestRouteTest with ImageRoutes {
  private lazy val routes = imageRoutes

  private def createImageFromBytes(imageData: Array[Byte]) = {
    try
      ImageIO.read(new ByteArrayInputStream(imageData))
    catch {
      case e: IOException =>
        throw new RuntimeException(e)
    }
  }

  // comparing 2 same sized images, returns true if no delta count > maxDelta
  private def compareImages(orig: BufferedImage, distorted: BufferedImage, xrange: Range, maxDelta: Int): Boolean = {

    def maxDistortion(argb: Int, argb2: Int): Int = {
      val (alpha1, rgb) = argbToComponents(argb)
      val (alpha2, rgb2) = argbToComponents(argb2)
      // in case alpha has been changed, should not
      if (alpha1 != alpha2) {
        Int.MaxValue
      } else {
        val zipped = rgb.zip(rgb2)
        zipped.map { case (c1, c2) => Math.abs(c1 - c2) }.max
      }
    }

    val height = orig.getHeight
    // using exists, this will bail early as soon as it finds a distortion too big
    val tooBig = (for {x <- xrange
                       y <- 0 until height} yield (x, y))
      .exists { case (x, y) =>
        val argb = orig.getRGB(x, y)
        val argb2 = distorted.getRGB(x, y)
        maxDistortion(argb, argb2) > maxDelta
      }
    !tooBig
  }

  "imageRoutes" should {
    "supply correct image" in {
      Get("/image") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        val byteString = entityAs[ByteString]
        val image = createImageFromBytes(byteString.toArray[Byte])

        // check same size as original image
        image.getWidth should ===(fbPNG.getWidth)
        image.getHeight should ===(fbPNG.getHeight)

        // maxDelta for left should be distortAmount
        val okLeft = compareImages(fbPNG, image, 0 to (image.getWidth - 1) / 2, distortAmount)
        okLeft should be(true)

        // maxDelta for right should be 0
        val okRight = compareImages(fbPNG, image, (image.getWidth - 1) / 2 + 1 until image.getWidth, 0)
        okRight should be(true)

      }
    }
    "fail" in {
      Get("/wrongurl") ~> Route.seal(routes) ~> check {
        status should ===(StatusCodes.NotFound)
      }
    }
  }
}
