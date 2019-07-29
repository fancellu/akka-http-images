import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, FileNotFoundException}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.http.scaladsl.server.Route
import javax.imageio.ImageIO

import scala.util.Random

trait ImageRoutes {

  val fbPNG: BufferedImage = pngToImage("facebook.png")

  implicit def system: ActorSystem

  def pngToImage(name: String): BufferedImage = {
    val imageResource = getClass.getResourceAsStream(name)
    if (imageResource == null) {
      // ok to throw exception here, we want the server to die if resource cannot be loaded
      throw new FileNotFoundException(name)
    }
    else {
      ImageIO.read(imageResource)
    }
  }

  def imageToByteArray(image: BufferedImage): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    ImageIO.write(image, "png", bos)
    bos.toByteArray
  }

  def deepCopy(bi: BufferedImage): BufferedImage = {
    val cm = bi.getColorModel
    val isAlphaPremultiplied = cm.isAlphaPremultiplied
    val raster = bi.copyData(null)
    new BufferedImage(cm, raster, isAlphaPremultiplied, null).getSubimage(0, 0, bi.getWidth, bi.getHeight)
  }

  private val random = Random

  def argbToComponents(argb:Int): (Int, List[Int])={
    ((argb >> 24) & 0xFF, List((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF))
  }

  def distortPixel(argb: Int, amount: Int): Int = {

    def distortComponent(n: Int): Int = {
      Math.max(0, n - random.nextInt(amount+1))
    }

    val (alpha, components) = argbToComponents(argb)
    val components2 = components.map(distortComponent)
    val r2 :: g2 :: b2 :: Nil = components2
    alpha << 24 | r2 << 16 | g2 << 8 | b2
  }

  def distortImage(image: BufferedImage, amount: Int): BufferedImage = {
    // you would hope there was a more efficient way of mapping the BufferedImage
    // would need more time to explore jvm image manipulation space
    val imageCopy = deepCopy(image)

    for {x <- 0 to (imageCopy.getWidth-1) / 2
         y <- 0 until imageCopy.getHeight} {
        val argb = imageCopy.getRGB(x, y)
        imageCopy.setRGB(x, y, distortPixel(argb, amount))
    }
    imageCopy
  }

  val distortAmount=5

  val imageRoutes: Route =
    path("image") {
      get {
        complete {
          val distorted = distortImage(fbPNG, distortAmount)
          HttpResponse(OK, entity = HttpEntity(ContentType(MediaTypes.`image/png`), imageToByteArray(distorted)))
        }
      }
    }
}
